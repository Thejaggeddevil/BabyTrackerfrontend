package com.example.babyparenting.ui.screens.millionaire

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import com.example.babyparenting.data.model.Activity
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel
import com.example.babyparenting.ui.theme.AppColorScheme
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.ui.viewmodel.ActivitiesUiState

// ──────────────────────────────────────────────────────────────────────────────
// StrategyDetailScreen
//
// ✅ LOCKING MECHANISM:
//    - First activity is ALWAYS unlocked
//    - Other activities are locked until the previous one is completed
//    - Once completed, activity stays unlocked
//
// ✅ COMPLETION STATUS:
//    - Each card shows if it's completed with a ✓ badge
//    - Locked cards show 🔒 icon
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyDetailScreen(
    strategyId: Int,
    strategyTitle: String,
    viewModel: MillionaireViewModel,
    onActivityClick: (activityId: Int, strategyId: Int) -> Unit,
    onBackClick: () -> Unit
) {
    val activitiesState by viewModel.activitiesState.collectAsState()
    val completedActivities by viewModel.completedActivities.collectAsState(initial = emptySet())
    val childAge by viewModel.childAge.collectAsState()

    val colors = LocalAppColors.current

    LaunchedEffect(strategyId) {
        viewModel.loadActivitiesForStrategy(strategyId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = strategyTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.bgSurface,
                scrolledContainerColor = colors.bgSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        when (val state = activitiesState) {
            is ActivitiesUiState.Success -> {

                // ✅ Filter activities by child age
                val filteredActivities = state.activities
                    .filter { activity ->
                        if (childAge == 0) {
                            true  // Show all when age is unknown
                        } else {
                            val min = activity.age_min ?: 0
                            val max = activity.age_max ?: 999
                            childAge in min..max
                        }
                    }
                    .sortedBy { it.level ?: 1 }

                if (filteredActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activities found for this age group",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    ActivitiesListWithoutLevelTabs(
                        activities = filteredActivities,
                        completedActivities = completedActivities,
                        colors = colors,
                        onActivityClick = { activity ->
                            onActivityClick(activity.id ?: 0, activity.strategy_id)
                        }
                    )
                }
            }

            is ActivitiesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.coral)
                }
            }

            is ActivitiesUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = colors.red,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {}
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ACTIVITIES LIST (NO LEVEL TABS — SIMPLE LINEAR LIST)
//
// ✅ LOCKING LOGIC:
//    - Activity at index 0 → ALWAYS UNLOCKED (entry point)
//    - Activity at index N > 0 → LOCKED if activity at index N-1 is NOT completed
//    - Once completed, activity stays unlocked
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActivitiesListWithoutLevelTabs(
    activities: List<Activity>,
    completedActivities: Set<Int>,
    colors: AppColorScheme,
    onActivityClick: (Activity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(activities) { activity ->
            val activityIndex = activities.indexOf(activity)

            // ✅ Locking logic: First activity unlocked, others locked until previous completed
            val isLocked = activityIndex > 0 &&
                    activities.getOrNull(activityIndex - 1)?.let {
                        !completedActivities.contains(it.id)
                    } ?: false

            val isCompleted = completedActivities.contains(activity.id)

            ActivityCard(
                activity = activity,
                isLocked = isLocked,
                isCompleted = isCompleted,
                colors = colors,
                onClick = {
                    if (!isLocked) {
                        onActivityClick(activity)
                    }
                }
            )
        }

        // Spacer
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ACTIVITY CARD (shows lock status, completion status)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActivityCard(
    activity: Activity,
    isLocked: Boolean,
    isCompleted: Boolean,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f)
    val alpha by animateFloatAsState(if (isLocked) 0.6f else 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isLocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = colors.bgSurface,
            disabledContainerColor = colors.bgSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.5.dp
        ),
        border = if (isCompleted) {
            BorderStroke(2.dp, colors.coral)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.title ?: "Activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )

                // ✅ STATUS BADGE
                when {
                    isCompleted -> {
                        Surface(
                            color = colors.coral.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = colors.coral,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                    isLocked -> {
                        Surface(
                            color = colors.textPrimary.copy(alpha = 0.1f),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = colors.textPrimary.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Description
            activity.description?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = colors.textPrimary.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }

            // Meta Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Age Range
                activity.age_min?.let { min ->
                    activity.age_max?.let { max ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("👶", fontSize = 10.sp)
                            Text(
                                text = "$min-$max m",
                                fontSize = 10.sp,
                                color = colors.textPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Duration
                activity.duration?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("⏱️", fontSize = 10.sp)
                        Text(
                            text = "${it}m",
                            fontSize = 10.sp,
                            color = colors.textPrimary.copy(alpha = 0.6f)
                        )
                    }
                }

                // Level
                activity.level?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("⭐", fontSize = 10.sp)
                        Text(
                            text = "L$it",
                            fontSize = 10.sp,
                            color = colors.textPrimary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = onClick,
                enabled = !isLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.coral,
                    disabledContainerColor = colors.textPrimary.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = when {
                        isCompleted -> "✓ Completed"
                        isLocked -> "🔒 Locked"
                        else -> "Start Activity"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Lock reason message
            if (isLocked) {
                Text(
                    text = "Complete previous activity to unlock",
                    fontSize = 9.sp,
                    color = colors.textPrimary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}