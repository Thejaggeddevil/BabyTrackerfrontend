package com.example.babyparenting.ui.screens.millionaire

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
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.data.model.Activity
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel
import com.example.babyparenting.ui.theme.AppColorScheme
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.ui.viewmodel.ActivitiesUiState

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
                Column {
                    Text(
                        text = strategyTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,  // ✅ FIXED: Use AutoMirrored version
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
                if (state.activities.isEmpty()) {
                    // ✅ FIXED: Handle empty activities state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activities found for this strategy",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    ActivitiesListWithLevelTabs(
                        activities = state.activities,
                        completedActivities = completedActivities,
                        colors = colors,
                        onActivityClick = { activity ->
                            onActivityClick(activity.id ?: 0, activity.strategy_id)  // ✅ FIXED: Handle nullable id
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

@Composable
private fun ActivitiesListWithLevelTabs(
    activities: List<Activity>,
    completedActivities: Set<Int>,
    colors: AppColorScheme,
    onActivityClick: (Activity) -> Unit
) {
    // ✅ FIXED: Better level extraction with safe defaults
    val levels = if (activities.isNotEmpty()) {
        activities.mapNotNull { it.level }.distinct().sorted()
    } else {
        listOf(1)
    }

    val pagerState = rememberPagerState(pageCount = { levels.size.coerceAtLeast(1) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        // ✅ FIXED: Only show tabs if there are multiple levels
        if (levels.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface),
                containerColor = colors.bgSurface,
                contentColor = colors.coral,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = colors.coral
                        )
                    }
                }
            ) {
                levels.forEachIndexed { index, level ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        text = {
                            Text(
                                text = "Level $level",
                                fontSize = 14.sp,
                                fontWeight = if (pagerState.currentPage == index)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            // ✅ FIXED: Safe level access
            val currentLevel = if (pageIndex < levels.size) levels[pageIndex] else levels.getOrNull(0) ?: 1
            val levelActivities = activities.filter { it.level == currentLevel }

            if (levelActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activities for Level $currentLevel",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(levelActivities) { activity ->
                        ActivityListItem(
                            activity = activity,
                            isCompleted = completedActivities.contains(activity.id ?: 0),  // ✅ FIXED: Handle nullable id
                            colors = colors,
                            onClick = { onActivityClick(activity) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityListItem(
    activity: Activity,
    isCompleted: Boolean,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                colors.coral.copy(alpha = 0.08f) else colors.bgSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isCompleted)
            BorderStroke(
                width = 2.dp,
                color = colors.coral
            ) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isCompleted) colors.coral
                        else colors.lavender.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = (activity.level ?: 1).toString(),  // ✅ FIXED: Handle nullable level
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = activity.title ?: "Activity",  // ✅ FIXED: Handle nullable title
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                // ✅ FIXED: Safe null check for description
                if (!activity.description.isNullOrEmpty()) {
                    Text(
                        text = activity.description ?: "",
                        fontSize = 12.sp,
                        color = colors.textPrimary.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }

                Text(
                    text = "${activity.duration ?: 30} min",  // ✅ FIXED: Use 'duration' instead of 'duration_min' with fallback
                    fontSize = 11.sp,
                    color = colors.textPrimary.copy(alpha = 0.5f)
                )
            }

            if (isCompleted) {
                Text(
                    text = "✓",
                    fontSize = 20.sp,
                    color = colors.coral,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}