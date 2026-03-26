package com.example.babyparenting.ui.screens.millionaire

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.data.model.DailyActivityResponse
import com.example.babyparenting.data.model.ProgressSummary
import com.example.babyparenting.data.model.Strategy
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel
import com.example.babyparenting.ui.theme.LocalAppColors

@Composable
fun MillionaireClubScreen(
    viewModel: MillionaireViewModel,
    onStrategyClick: (strategyId: Int) -> Unit,
    onActivityClick: (activityId: Int, strategyId: Int) -> Unit
) {
    val strategiesState by viewModel.strategiesState.collectAsState()
    val dailyActivityState by viewModel.dailyActivityState.collectAsState()
    val progressState by viewModel.progressState.collectAsState()
    val colors = LocalAppColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ===== Header =====
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Millionaire Baby Club",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Build thinking skills through daily activities",
                    fontSize = 14.sp,
                    color = colors.textSecondary ?: colors.textPrimary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // ===== Today's Activity Card =====
        item {
            when (val state = dailyActivityState) {
                is DailyActivityUiState.Success -> {
                    TodaysActivityCard(
                        activity = state.activity,
                        colors = colors,
                        onActivityClick = { onActivityClick(state.activity.activity_id, state.activity.strategy_id) }
                    )
                }
                is DailyActivityUiState.Loading -> {
                    LoadingCard()
                }
                is DailyActivityUiState.Error -> {
                    ErrorCard(message = state.message)
                }
            }
        }

        // ===== Progress Summary =====
        item {
            when (val state = progressState) {
                is ProgressUiState.Success -> {
                    ProgressSummaryCard(
                        progress = state.progress,
                        colors = colors
                    )
                }
                is ProgressUiState.Loading -> {
                    LoadingCard()
                }
                is ProgressUiState.Error -> {
                    // Silent failure for progress (optional)
                }
                else -> {}
            }
        }

        // ===== Strategies Section Header =====
        item {
            Text(
                text = "Learning Strategies",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ===== Strategies Horizontal List =====
        item {
            when (val state = strategiesState) {
                is StrategiesUiState.Success -> {
                    StrategiesHorizontalList(
                        strategies = state.strategies,
                        colors = colors,
                        onStrategyClick = onStrategyClick
                    )
                }
                is StrategiesUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                is StrategiesUiState.Error -> {
                    ErrorCard(message = state.message)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TodaysActivityCard(
    activity: DailyActivityResponse,
    colors: com.example.babyparenting.ui.theme.AppColors,
    onActivityClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onActivityClick),
        colors = CardDefaults.cardColors(
            containerColor = colors.coral.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Activity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                    Text(
                        text = activity.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open activity",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Level ${activity.level} • 10 min",
                fontSize = 12.sp,
                color = colors.textPrimary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ProgressSummaryCard(
    progress: ProgressSummary,
    colors: com.example.babyparenting.ui.theme.AppColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${progress.completed_activities}/${progress.total_activities}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = progress.completion_percentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.2f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProgressStatItem("Completed", progress.completed_activities.toString(), colors)
                ProgressStatItem("Level", "${progress.current_level}", colors)
            }
        }
    }
}

@Composable
private fun ProgressStatItem(
    label: String,
    value: String,
    colors: com.example.babyparenting.ui.theme.AppColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .background(colors.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textPrimary.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StrategiesHorizontalList(
    strategies: List<Strategy>,
    colors: com.example.babyparenting.ui.theme.AppColors,
    onStrategyClick: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(strategies) { strategy ->
            StrategyCard(
                strategy = strategy,
                colors = colors,
                onClick = { onStrategyClick(strategy.id) }
            )
        }
    }
}

@Composable
private fun StrategyCard(
    strategy: Strategy,
    colors: com.example.babyparenting.ui.theme.AppColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strategy.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2
                )
                Text(
                    text = "${strategy.age_min}–${strategy.age_max} years",
                    fontSize = 11.sp,
                    color = colors.textPrimary.copy(alpha = 0.6f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${strategy.completed_count}/${strategy.total_activities}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
                LinearProgressIndicator(
                    progress = if (strategy.total_activities > 0)
                        strategy.completed_count.toFloat() / strategy.total_activities else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colors.primary,
                    trackColor = colors.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp)
            .background(LocalAppColors.current.bgSurface, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = LocalAppColors.current.primary)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = LocalAppColors.current.red.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Error",
                tint = LocalAppColors.current.red,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = LocalAppColors.current.red,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
