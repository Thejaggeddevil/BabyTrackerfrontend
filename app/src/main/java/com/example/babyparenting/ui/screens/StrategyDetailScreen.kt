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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
// STRATEGY DETAIL SCREEN
//
// ✅ Activities 1–10 shown as a journey:
//    - #1 always unlocked
//    - #N locked until #N-1 is completed
//    - Next locked activities are dimmed but visible
//    - Completed ones show ✓ badge + coral border
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
        viewModel.loadCompletedActivities(userId)  // ✅ Har baar completed IDs reload karo
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = strategyTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Activities",
                        fontSize = 11.sp,
                        color = colors.textPrimary.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.bgSurface
            )
        )

        when (val state = activitiesState) {

            is ActivitiesUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.coral, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading activities...",
                            fontSize = 13.sp,
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
                        Text("⚠️", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message,
                            fontSize = 14.sp,
                            color = colors.red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadActivitiesForStrategy(strategyId) },
                            colors  = ButtonDefaults.buttonColors(containerColor = colors.coral)
                        ) { Text("Retry") }
                    }
                }
            }

            is ActivitiesUiState.Success -> {
                // Filter + sort
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
                            Text("👶", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No activities for this age group",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Header summary
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ── Progress summary header ──────────────────────────────────────────
        item {
            ProgressHeader(
                completed = completedCount,
                total     = activities.size,
                colors    = colors
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Activity items ───────────────────────────────────────────────────
        itemsIndexed(activities) { index, activity ->
            val isCompleted = completedActivities.contains(activity.id)
            val isLocked = index > 0 &&
                    !completedActivities.contains(activities[index - 1].id)
            val isNext = !isCompleted && !isLocked &&
                    (index == 0 || completedActivities.contains(activities[index - 1].id))

            ActivityJourneyItem(
                activity    = activity,
                index       = index,
                isLast      = index == activities.lastIndex,
                isCompleted = isCompleted,
                isLocked    = isLocked,
                isNext      = isNext,
                colors      = colors,
                onClick = {
                    if (!isLocked) {
                        onActivityClick(activity.id ?: 0, strategyId)
                    }
                }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS HEADER CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProgressHeader(
    completed: Int,
    total: Int,
    colors: AppColorScheme
) {
    val progress by animateFloatAsState(
        targetValue   = if (total > 0) completed.toFloat() / total else 0f,
        animationSpec = tween(600)
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.coral.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Strategy Progress",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Surface(
                    color    = colors.coral.copy(alpha = 0.15f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Text(
                        "$completed / $total",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.coral,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress  = progress,
                modifier  = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color      = colors.coral,
                trackColor = colors.coral.copy(alpha = 0.15f)
            )

            if (completed == total && total > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🎉", fontSize = 14.sp)
                    Text(
                        "Strategy completed!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.coral
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITY JOURNEY ITEM  —  node + connector + card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActivityJourneyItem(
    activity: Activity,
    index: Int,
    isLast: Boolean,
    isCompleted: Boolean,
    isLocked: Boolean,
    isNext: Boolean,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isLocked) 0.5f else 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ── Left: node + connector ───────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Pulse animation for "next" activity
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue  = if (isNext) 1.15f else 1f,
                animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse)
            )

            // Node circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> colors.coral
                            isNext      -> colors.coral.copy(alpha = 0.2f)
                            else        -> colors.bgSurface
                        }
                    )
                    .border(
                        width = if (isNext) 2.dp else 1.5.dp,
                        color = when {
                            isCompleted -> colors.coral
                            isNext      -> colors.coral
                            isLocked    -> colors.textPrimary.copy(alpha = 0.15f)
                            else        -> colors.textPrimary.copy(alpha = 0.2f)
                        },
                        shape = CircleShape
                    )
            ) {
                when {
                    isCompleted -> Icon(
                        Icons.Default.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    isLocked -> Icon(
                        Icons.Default.Lock,
                        null,
                        tint = colors.textPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Text(
                        "${index + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNext) colors.coral else colors.textPrimary.copy(alpha = 0.5f)
                    )
                }
            }

            // Vertical connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if ((activity.basic?.plan?.length ?: 0) > 80) 120.dp else 90.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isCompleted) colors.coral else colors.coral.copy(alpha = 0.25f),
                                    colors.coral.copy(alpha = 0.05f)
                                )
                            )
                        )
                )
            }
        }

        // ── Right: Activity card ─────────────────────────────────────────────
        ActivityItemCard(
            activity    = activity,
            isCompleted = isCompleted,
            isLocked    = isLocked,
            isNext      = isNext,
            alpha       = alpha,
            colors      = colors,
            onClick     = onClick
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
    alpha: Float,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()  // ← weight(1f) ki jagah yeh lagao
            .padding(bottom = 12.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isLocked, onClick = onClick),
        colors    = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> colors.coral.copy(alpha = 0.07f)
                isNext      -> colors.bgSurface
                else        -> colors.bgSurface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (isNext) 3.dp else 1.5.dp),
        border = when {
            isCompleted -> BorderStroke(1.5.dp, colors.coral.copy(alpha = 0.5f))
            isNext      -> BorderStroke(1.5.dp, colors.coral.copy(alpha = 0.35f))
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
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    lineHeight = 17.sp
                )

                // Status chip
                when {
                    isCompleted -> StatusChip("✓ Done", colors.coral, colors)
                    isNext      -> StatusChip("▶ Next", colors.coral.copy(alpha = 0.7f), colors)
                    isLocked    -> StatusChip("🔒", colors.textPrimary.copy(alpha = 0.4f), colors)
                }
            }

            // Plan preview
            activity.basic?.plan?.let { plan ->
                if (plan.isNotBlank()) {
                    Text(
                        text = plan,
                        fontSize = 11.sp,
                        color = colors.textPrimary.copy(alpha = 0.6f),
                        maxLines = 2,
                        lineHeight = 15.sp
                    )
                }
            }

            // Meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activity.meta?.timeMinutes?.let {
                    MiniChip("⏱️ ${it}m", colors)
                }
                activity.meta?.materials?.size?.let { count ->
                    if (count > 0) MiniChip("🧩 $count items", colors)
                }
                Spacer(Modifier.weight(1f))
                // Level
                Text(
                    "L${activity.level}",
                    fontSize = 9.sp,
                    color = colors.coral.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action button
            Button(
                onClick  = onClick,
                enabled  = !isLocked,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = when {
                        isCompleted -> colors.textPrimary.copy(alpha = 0.08f)
                        else        -> colors.coral
                    },
                    disabledContainerColor = colors.textPrimary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = when {
                        isCompleted -> "✓ Completed"
                        isLocked    -> "🔒 Complete previous first"
                        else        -> "Start Activity →"
                    },
                    fontSize     = 11.sp,
                    fontWeight   = FontWeight.SemiBold,
                    color = when {
                        isCompleted -> colors.textPrimary.copy(alpha = 0.4f)
                        isLocked    -> colors.textPrimary.copy(alpha = 0.35f)
                        else        -> Color.White
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color, colors: AppColorScheme) {
    Surface(
        color    = color.copy(alpha = 0.12f),
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MiniChip(text: String, colors: AppColorScheme) {
    Surface(
        color    = colors.bgMain,
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = colors.textPrimary.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}