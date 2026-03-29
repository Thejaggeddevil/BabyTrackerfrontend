package com.example.babyparenting.ui.screens.millionaire

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.data.model.ProgressSummary
import com.example.babyparenting.data.model.Strategy
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.ui.viewmodel.DailyActivityUiState
import com.example.babyparenting.ui.viewmodel.ProgressUiState
import com.example.babyparenting.ui.viewmodel.StrategiesUiState

@Composable
fun MillionaireClubScreen(
    viewModel: MillionaireViewModel,
    onStrategyClick: (strategyId: Int) -> Unit,
    childAge: Int,
    onActivityClick: (activityId: Int, strategyId: Int) -> Unit
) {
    val strategiesState by viewModel.strategiesState.collectAsState()
    val dailyActivityState by viewModel.dailyActivityState.collectAsState()
    val progressState by viewModel.progressState.collectAsState()
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        // ────────────────────────────────────────────────────────────────────
        // HEADER (Clean - No Search/Filter)
        // ────────────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSurface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Millionaire Baby Club",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "Build thinking skills daily",
                fontSize = 11.sp,
                color = colors.textPrimary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal
            )
        }

        // ────────────────────────────────────────────────────────────────────
        // MAIN CONTENT
        // ────────────────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1️⃣ DAILY ACTIVITY SECTION
            item {
                DailyActivitySection(
                    dailyActivityState = dailyActivityState,
                    colors = colors,
                    onActivityClick = onActivityClick
                )
            }

            // 2️⃣ PROGRESS SECTION
            item {
                when (val state = progressState) {
                    is ProgressUiState.Success -> {
                        ProgressSummarySection(
                            progress = state.progress,
                            colors = colors
                        )
                    }
                    is ProgressUiState.Loading -> {
                        LoadingCard()
                    }
                    is ProgressUiState.Error -> {
                        // Silently skip on error
                    }
                }
            }

            // 3️⃣ STRATEGIES SECTION HEADER
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Strategies",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }

            // 4️⃣ STRATEGIES LIST
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
                        LoadingCard()
                    }

                    is StrategiesUiState.Error -> {
                        ErrorCard(state.message)
                    }
                }
            }

            // Spacer for bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 📅 DAILY ACTIVITY SECTION (Uses ActivityDetail from DailyActivityResponse)
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun DailyActivitySection(
    dailyActivityState: DailyActivityUiState,
    colors: com.example.babyparenting.ui.theme.AppColorScheme,
    onActivityClick: (activityId: Int, strategyId: Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                text = "📅 Today's Activity",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            // Content
            when (val state = dailyActivityState) {
                is DailyActivityUiState.Success -> {
                    val activityDetail = state.activity?.activity

                    if (activityDetail != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = activityDetail.title ?: "",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )

                            if (!activityDetail.plan.isNullOrBlank()) {
                                Text(
                                    text = activityDetail.plan ?: "",
                                    fontSize = 12.sp,
                                    color = colors.textPrimary.copy(alpha = 0.7f),
                                    maxLines = 2
                                )
                            }

                            Button(
                                onClick = {
                                    val id = activityDetail.id ?: return@Button
                                    onActivityClick(id, activityDetail.strategy_id)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.coral),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Start Activity",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                    } else {
                        Text(
                            text = "No activity scheduled for today",
                            fontSize = 12.sp,
                            color = colors.textPrimary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    }
                }

                is DailyActivityUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.coral,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                is DailyActivityUiState.Error -> {
                    Text(
                        text = "Could not load today's activity",
                        fontSize = 12.sp,
                        color = colors.red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 📊 PROGRESS SUMMARY SECTION
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProgressSummarySection(
    progress: ProgressSummary,
    colors: com.example.babyparenting.ui.theme.AppColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Your Progress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Surface(
                    color = colors.coral.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 11.sp)
                        Text(
                            text = "Level ${progress.current_level}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.coral
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = (progress.completion_percentage / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colors.coral,
                trackColor = colors.coral.copy(alpha = 0.2f)
            )

            Text(
                text = "${progress.completed_activities}/${progress.total_activities} completed (${progress.completion_percentage.toInt()}%)",
                fontSize = 11.sp,
                color = colors.textPrimary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// STRATEGIES HORIZONTAL LIST
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun StrategiesHorizontalList(
    strategies: List<Strategy>,
    colors: com.example.babyparenting.ui.theme.AppColorScheme,
    onStrategyClick: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(strategies.size) { index ->
            strategies.getOrNull(index)?.let { strategy ->
                StrategyCard(
                    strategy = strategy,
                    colors = colors,
                    onClick = { onStrategyClick(strategy.id) }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// STRATEGY CARD (with completion badge)
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun StrategyCard(
    strategy: Strategy,
    colors: com.example.babyparenting.ui.theme.AppColorScheme,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f)

    Card(
        modifier = Modifier
            .width(160.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strategy.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 2
                        )
                    }

                    // ✅ Show completion badge
                    if (strategy.completed_count > 0 && strategy.completed_count == strategy.total_activities) {
                        Surface(
                            color = colors.coral.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.coral,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("👶", fontSize = 10.sp)
                    Text(
                        text = "${strategy.age_min}–${strategy.age_max}",
                        fontSize = 10.sp,
                        color = colors.textPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${strategy.completed_count}/${strategy.total_activities}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.coral
                )
                LinearProgressIndicator(
                    progress = if (strategy.total_activities > 0)
                        (strategy.completed_count.toFloat() / strategy.total_activities).coerceIn(0f, 1f)
                    else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.coral,
                    trackColor = colors.coral.copy(alpha = 0.2f)
                )
            }

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.coral),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "View All",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// LOADING & ERROR CARDS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LocalAppColors.current.bgSurface),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = LocalAppColors.current.coral)
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
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Error",
                tint = LocalAppColors.current.red,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                fontSize = 11.sp,
                color = LocalAppColors.current.red,
                modifier = Modifier.weight(1f)
            )
        }
    }
}