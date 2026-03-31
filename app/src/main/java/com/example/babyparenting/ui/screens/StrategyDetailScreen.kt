package com.example.babyparenting.ui.screens.millionaire

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.data.model.Activity
import com.example.babyparenting.ui.theme.AppColorScheme
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.ui.viewmodel.ActivitiesUiState
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel

// ─────────────────────────────────────────────────────────────────────────────
// PALETTE HELPERS  (lavender tints used for "completed" state)
// ─────────────────────────────────────────────────────────────────────────────
private val LavenderBase   = Color(0xFFB39DDB)
private val LavenderLight  = Color(0xFFF3EFFF)
private val LavenderBorder = Color(0xFFCEC3F5)
private val LavenderChip   = Color(0xFF9E8DD0)

// ─────────────────────────────────────────────────────────────────────────────
// STRATEGY DETAIL SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyDetailScreen(
    strategyId: Int,
    strategyTitle: String,
    viewModel: MillionaireViewModel,
    onActivityClick: (activityId: Int, strategyId: Int) -> Unit,
    onBackClick: () -> Unit
) {
    val activitiesState     by viewModel.activitiesState.collectAsState()
    val completedActivities by viewModel.completedActivities.collectAsState()
    val childAge            by viewModel.childAge.collectAsState()
    val colors = LocalAppColors.current

    LaunchedEffect(strategyId) {
        viewModel.loadActivitiesForStrategy(strategyId)
        val userId = com.example.babyparenting.data.local.UserManager.getUserId(
            viewModel.getContext()
        )
        viewModel.loadCompletedActivities(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = strategyTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Activities",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgSurface)
        )

        when (val state = activitiesState) {

            is ActivitiesUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.coral, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading activities...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            is ActivitiesUiState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.message,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.loadActivitiesForStrategy(strategyId) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.coral),
                            modifier = Modifier.height(48.dp).clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            is ActivitiesUiState.Success -> {
                val activities = state.activities
                    .filter { activity ->
                        if (childAge == 0) true
                        else {
                            val min = activity.age_min ?: 0
                            val max = activity.age_max ?: 999
                            (childAge / 12) in min..max
                        }
                    }
                    .sortedBy { it.level ?: 1 }

                if (activities.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👶", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No activities for this age group",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val completedCount = activities.count { completedActivities.contains(it.id) }
                    ActivitiesContent(
                        activities          = activities,
                        completedActivities = completedActivities,
                        completedCount      = completedCount,
                        strategyId          = strategyId,
                        colors              = colors,
                        onActivityClick     = onActivityClick
                    )
                }
            }

            else -> {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITIES CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActivitiesContent(
    activities: List<Activity>,
    completedActivities: Set<Int>,
    completedCount: Int,
    strategyId: Int,
    colors: AppColorScheme,
    onActivityClick: (Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ── Header ──────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.coral.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, colors.coral.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Your Journey",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary.copy(alpha = 0.6f)
                        )
                        Text(
                            "$completedCount of ${activities.size} completed",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.coral
                        )
                    }
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / activities.size },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .padding(start = 16.dp),
                        color = colors.coral,
                        trackColor = colors.textPrimary.copy(alpha = 0.1f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // ── Zigzag Activities ────────────────────────────────────────────────
        itemsIndexed(activities) { index, activity ->
            val isCompleted = completedActivities.contains(activity.id)
            val isLocked    = index > 0 && !completedActivities.contains(activities[index - 1].id)
            val isNext      = index > 0 && !isLocked && !isCompleted
            val isLast      = index == activities.size - 1

            // ✅ ZIGZAG: index 0,2,4... → circle LEFT, card RIGHT
            //            index 1,3,5... → card LEFT, circle RIGHT
            val circleOnLeft = index % 2 == 0

            ZigzagActivityItem(
                activity      = activity,
                index         = index,
                isCompleted   = isCompleted,
                isLocked      = isLocked,
                isNext        = isNext,
                isLast        = isLast,
                circleOnLeft  = circleOnLeft,
                colors        = colors,
                onClick       = { onActivityClick(activity.id!!, strategyId) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ZIGZAG ACTIVITY ITEM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ZigzagActivityItem(
    activity: Activity,
    index: Int,
    isCompleted: Boolean,
    isLocked: Boolean,
    isNext: Boolean,
    isLast: Boolean,
    circleOnLeft: Boolean,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    val alpha = if (isLocked) 0.55f else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        // ── Card + Circle row ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (circleOnLeft) {
                // index 0,2,4... → ⬤ Card
                ActivityCircle(
                    index       = index,
                    isCompleted = isCompleted,
                    isNext      = isNext,
                    isLocked    = isLocked,
                    colors      = colors
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ActivityItemCard(
                        activity    = activity,
                        isCompleted = isCompleted,
                        isLocked    = isLocked,
                        isNext      = isNext,
                        colors      = colors,
                        onClick     = onClick
                    )
                }
            } else {
                // index 1,3,5... → Card ⬤
                Box(modifier = Modifier.weight(1f)) {
                    ActivityItemCard(
                        activity    = activity,
                        isCompleted = isCompleted,
                        isLocked    = isLocked,
                        isNext      = isNext,
                        colors      = colors,
                        onClick     = onClick
                    )
                }
                Spacer(Modifier.width(10.dp))
                ActivityCircle(
                    index       = index,
                    isCompleted = isCompleted,
                    isNext      = isNext,
                    isLocked    = isLocked,
                    colors      = colors
                )
            }
        }

        // ── Curved connector to next item (opposite side) ────────────────────
        if (!isLast) {
            ZigzagConnector(
                fromLeft    = circleOnLeft,   // connector starts from current circle side
                isCompleted = isCompleted,
                colors      = colors
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CIRCLE INDICATOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActivityCircle(
    index: Int,
    isCompleted: Boolean,
    isNext: Boolean,
    isLocked: Boolean,
    colors: AppColorScheme
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCompleted -> LavenderBase.copy(alpha = 0.25f)
                    isNext      -> colors.coral.copy(alpha = 0.2f)
                    isLocked    -> colors.textPrimary.copy(alpha = 0.08f)
                    else        -> colors.bgSurface
                }
            )
            .border(
                width = if (isNext || isCompleted) 2.5.dp else 2.dp,
                color = when {
                    isCompleted -> LavenderBorder
                    isNext      -> colors.coral
                    isLocked    -> colors.textPrimary.copy(alpha = 0.2f)
                    else        -> colors.textPrimary.copy(alpha = 0.15f)
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isCompleted -> Icon(
                Icons.Default.Check,
                contentDescription = "Completed",
                tint = LavenderChip,
                modifier = Modifier.size(20.dp)
            )
            else -> Text(
                "${index + 1}",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isNext) colors.coral else colors.textPrimary.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ZIGZAG CONNECTOR  — curved dashed line from one circle to the next
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ZigzagConnector(
    fromLeft: Boolean,    // true = current circle is on left → next circle on right
    isCompleted: Boolean,
    colors: AppColorScheme
) {
    val lineColor = if (isCompleted) LavenderBorder.copy(alpha = 0.7f)
    else colors.coral.copy(alpha = 0.3f)

    val circleSize = 44.dp   // must match ActivityCircle size

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val circleRadius = circleSize.toPx() / 2f

        // Circle center X — circle sits at very left or very right of the row
        val startX = if (fromLeft) circleRadius else size.width - circleRadius
        val endX   = if (fromLeft) size.width - circleRadius else circleRadius

        val startY = 0f
        val endY   = size.height

        val path = Path().apply {
            moveTo(startX, startY)
            cubicTo(
                startX, startY + size.height * 0.5f,   // control point 1
                endX,   endY   - size.height * 0.5f,   // control point 2
                endX,   endY
            )
        }

        drawPath(
            path  = path,
            color = lineColor,
            style = Stroke(
                width      = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITY ITEM CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActivityItemCard(
    activity: Activity,
    isCompleted: Boolean,
    isLocked: Boolean,
    isNext: Boolean,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isLocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> LavenderLight
                else        -> colors.bgSurface
            }
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNext) 3.5.dp else 1.5.dp
        ),
        border = when {
            isCompleted -> BorderStroke(1.5.dp, LavenderBorder)
            isNext      -> BorderStroke(2.dp, colors.coral.copy(alpha = 0.4f))
            else        -> null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = activity.title ?: "Activity ${activity.id}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    lineHeight = 17.sp
                )
                when {
                    isCompleted -> StatusChip("✓ Done", LavenderChip, colors)
                    isNext      -> StatusChip("▶ Next", colors.coral.copy(alpha = 0.8f), colors)
                    isLocked    -> StatusChip("🔒", colors.textPrimary.copy(alpha = 0.35f), colors)
                }
            }

            // Plan preview
            activity.basic?.plan?.let { plan ->
                if (plan.isNotBlank()) {
                    Text(
                        text = plan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary.copy(alpha = 0.6f),
                        maxLines = 2,
                        lineHeight = 15.sp
                    )
                }
            }

            // Meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activity.meta?.timeMinutes?.let {
                    MiniChip("⏱️ ${it}m", colors)
                }
                activity.meta?.materials?.size?.let { count ->
                    if (count > 0) MiniChip("🧩 $count", colors)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "L${activity.level}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCompleted) LavenderChip.copy(alpha = 0.7f)
                    else colors.coral.copy(alpha = 0.7f)
                )
            }

            // Action button
            Button(
                onClick = onClick,
                enabled = !isLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isCompleted -> LavenderBase.copy(alpha = 0.18f)
                        else        -> colors.coral
                    },
                    disabledContainerColor = colors.textPrimary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (!isLocked && !isCompleted) 3.dp else 0.dp,
                    pressedElevation = if (!isLocked && !isCompleted) 6.dp else 0.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            isCompleted -> "✓ Completed"
                            isLocked    -> "🔒 Complete previous"
                            else        -> "Start Activity"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isCompleted -> LavenderChip
                            isLocked    -> colors.textPrimary.copy(alpha = 0.35f)
                            else        -> Color.White
                        }
                    )
                    if (!isCompleted && !isLocked) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatusChip(text: String, color: Color, colors: AppColorScheme) {
    Surface(
        color = color.copy(alpha = 0.14f),
        modifier = Modifier
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(0.8.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MiniChip(text: String, colors: AppColorScheme) {
    Surface(
        color = colors.bgMain,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(0.8.dp, colors.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}