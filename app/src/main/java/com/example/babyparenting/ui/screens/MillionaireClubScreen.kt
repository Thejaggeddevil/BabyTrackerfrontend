package com.example.babyparenting.ui.screens.millionaire

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.data.model.ProgressSummary
import com.example.babyparenting.data.model.Strategy
import com.example.babyparenting.ui.theme.AppColorScheme
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.ui.viewmodel.*

// ─────────────────────────────────────────────────────────────────────────────
// MILLIONAIRE CLUB MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MillionaireClubScreen(
    viewModel: MillionaireViewModel,
    onStrategyClick: (strategyId: Int) -> Unit,
    childAge: Int,
    onActivityClick: (activityId: Int, strategyId: Int) -> Unit
) {
    val strategiesState      by viewModel.strategiesState.collectAsState()
    val dailyActivityState   by viewModel.dailyActivityState.collectAsState()
    val progressState        by viewModel.progressState.collectAsState()
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        ClubHeader(colors = colors)

        // ── Content ───────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // 1. TODAY'S ACTIVITY
            item {
                TodayActivitySection(
                    state       = dailyActivityState,
                    colors      = colors,
                    onActivityClick = onActivityClick
                )
            }

            // 2. PROGRESS BAR
            item {
                when (val s = progressState) {
                    is ProgressUiState.Success ->
                        ProgressSection(progress = s.progress, colors = colors)
                    is ProgressUiState.Loading -> LoadingPlaceholder(colors)
                    else -> {}
                }
            }

            // 3. STRATEGIES HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Learning Strategies",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            "Tap a strategy to explore activities",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.5f)
                        )
                    }
                    when (val s = strategiesState) {
                        is StrategiesUiState.Success ->
                            Text(
                                "${s.strategies.size} total",
                                fontSize = 11.sp,
                                color = colors.coral,
                                fontWeight = FontWeight.SemiBold
                            )
                        else -> {}
                    }
                }
            }

            // 4. STRATEGIES LIST
            item {
                when (val s = strategiesState) {
                    is StrategiesUiState.Success ->
                        StrategiesJourneyList(
                            strategies     = s.strategies,
                            colors         = colors,
                            onStrategyClick = onStrategyClick
                        )
                    is StrategiesUiState.Loading -> LoadingPlaceholder(colors)
                    is StrategiesUiState.Error   -> ErrorPlaceholder(s.message, colors)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClubHeader(colors: AppColorScheme) {
    Surface(
        color = colors.bgSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "🌟 Millionaire Baby Club",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )
                Text(
                    "Build thinking skills every day",
                    fontSize = 11.sp,
                    color = colors.textPrimary.copy(alpha = 0.55f)
                )
            }
            // Animated star
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue  = 8f,
                animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse)
            )
            Text("⭐", fontSize = 28.sp, modifier = Modifier.rotate(rotation))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TODAY'S ACTIVITY SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayActivitySection(
    state: DailyActivityUiState,
    colors: AppColorScheme,
    onActivityClick: (Int, Int) -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.coral.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color    = colors.coral.copy(alpha = 0.15f),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            "📅 TODAY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.coral,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        "Activity of the Day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                when (state) {

                    is DailyActivityUiState.Success -> {
                        val detail = state.activity.activity
                        if (detail != null) {

                            // Title
                            Text(
                                text = detail.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            // Steps preview
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniStep("🎯", "Plan",   detail.plan,   colors)
                                MiniStep("▶️", "Do",     detail.`do`,   colors)
                                MiniStep("🔁", "Review", detail.review, colors)
                            }

                            // Meta + button row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                detail.duration?.let {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("⏱️", fontSize = 11.sp)
                                        Text(
                                            "${it}m",
                                            fontSize = 11.sp,
                                            color = colors.textPrimary.copy(alpha = 0.6f)
                                        )
                                    }
                                } ?: Spacer(Modifier.width(1.dp))

                                Button(
                                    onClick = {
                                        onActivityClick(detail.id, detail.strategy_id)
                                    },
                                    colors   = ButtonDefaults.buttonColors(containerColor = colors.coral),
                                    shape    = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Start",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                        } else {
                            EmptyDailyActivity(colors)
                        }
                    }

                    is DailyActivityUiState.Loading -> {
                        Box(
                            Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = colors.coral,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }

                    is DailyActivityUiState.Error -> {
                        Text(
                            "Could not load today's activity",
                            fontSize = 12.sp,
                            color = colors.red,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStep(
    emoji: String,
    label: String,
    text: String?,
    colors: AppColorScheme
) {
    if (text.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgMain)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(emoji, fontSize = 12.sp)
        Column {
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.coral
            )
            Text(
                text,
                fontSize = 11.sp,
                color = colors.textPrimary.copy(alpha = 0.75f),
                maxLines = 2,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun EmptyDailyActivity(colors: AppColorScheme) {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("🎉", fontSize = 28.sp)
            Text(
                "All activities completed!",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProgressSection(
    progress: ProgressSummary,
    colors: AppColorScheme
) {
    val animProgress by animateFloatAsState(
        targetValue   = (progress.completion_percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(800)
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📊 Your Progress",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Surface(
                    color    = colors.coral.copy(alpha = 0.15f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 10.sp)
                        Text(
                            "Level ${progress.current_level}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.coral
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress  = animProgress,
                modifier  = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color      = colors.coral,
                trackColor = colors.coral.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${progress.completed_activities} completed",
                    fontSize = 11.sp,
                    color = colors.textPrimary.copy(alpha = 0.6f)
                )
                Text(
                    "${progress.completion_percentage.toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.coral
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STRATEGIES — Journey-style vertical list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StrategiesJourneyList(
    strategies: List<Strategy>,
    colors: AppColorScheme,
    onStrategyClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        strategies.forEachIndexed { index, strategy ->
            StrategyJourneyItem(
                strategy      = strategy,
                index         = index,
                isLast        = index == strategies.lastIndex,
                colors        = colors,
                onClick       = { onStrategyClick(strategy.id) }
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StrategyJourneyItem(
    strategy: Strategy,
    index: Int,
    isLast: Boolean,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    val isCompleted = strategy.completed_count > 0 &&
            strategy.completed_count == strategy.total_activities
    val hasProgress = strategy.completed_count > 0 && !isCompleted
    val isLeft = index % 2 == 0

    // Alternate left/right layout like journey map
    Row(
        modifier  = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (!isLeft) Spacer(Modifier.weight(1f))

        Column(
            modifier  = Modifier.weight(1.4f),
            horizontalAlignment = if (isLeft) Alignment.Start else Alignment.End
        ) {
            // Node + connector
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = if (isLeft)
                    Arrangement.Start else Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isLeft) {
                    // Connector line (right side)
                    StrategyCard(
                        strategy  = strategy,
                        isLeft    = false,
                        isCompleted = isCompleted,
                        hasProgress = hasProgress,
                        colors    = colors,
                        onClick   = onClick
                    )
                    Spacer(Modifier.width(8.dp))
                }

                // Node circle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> colors.coral
                                    hasProgress -> colors.coral.copy(alpha = 0.4f)
                                    else        -> colors.bgSurface
                                }
                            )
                            .border(
                                2.dp,
                                if (isCompleted || hasProgress) colors.coral
                                else colors.textPrimary.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        when {
                            isCompleted -> Icon(
                                Icons.Default.Check,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            else -> Text(
                                "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasProgress) colors.coral else colors.textPrimary.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Connector line going down
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(if (strategy.description.length > 60) 90.dp else 70.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            if (isCompleted) colors.coral else colors.coral.copy(alpha = 0.3f),
                                            colors.coral.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                        )
                    }
                }

                if (isLeft) {
                    Spacer(Modifier.width(8.dp))
                    StrategyCard(
                        strategy    = strategy,
                        isLeft      = true,
                        isCompleted = isCompleted,
                        hasProgress = hasProgress,
                        colors      = colors,
                        onClick     = onClick
                    )
                }
            }
        }

        if (isLeft) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun StrategyCard(
    strategy: Strategy,
    isLeft: Boolean,
    isCompleted: Boolean,
    hasProgress: Boolean,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> colors.coral.copy(alpha = 0.1f)
                hasProgress -> colors.bgSurface
                else        -> colors.bgSurface
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (isCompleted)
            BorderStroke(1.5.dp, colors.coral.copy(alpha = 0.4f))
        else if (hasProgress)
            BorderStroke(1.dp, colors.coral.copy(alpha = 0.2f))
        else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = strategy.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 2,
                lineHeight = 16.sp
            )

            if (strategy.description.isNotBlank()) {
                Text(
                    text = strategy.description,
                    fontSize = 10.sp,
                    color = colors.textPrimary.copy(alpha = 0.55f),
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }

            // Progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${strategy.completed_count}/${strategy.total_activities}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.coral
                )
                Text(
                    "Age ${strategy.age_min}–${strategy.age_max}",
                    fontSize = 9.sp,
                    color = colors.textPrimary.copy(alpha = 0.4f)
                )
            }

            // Mini progress bar
            LinearProgressIndicator(
                progress = if (strategy.total_activities > 0)
                    (strategy.completed_count.toFloat() / strategy.total_activities).coerceIn(0f, 1f)
                else 0f,
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color      = colors.coral,
                trackColor = colors.coral.copy(alpha = 0.15f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingPlaceholder(colors: AppColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgSurface),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colors.coral, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ErrorPlaceholder(message: String, colors: AppColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors   = CardDefaults.cardColors(containerColor = colors.red.copy(alpha = 0.08f)),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = colors.red,
            modifier = Modifier.padding(14.dp),
            textAlign = TextAlign.Center
        )
    }
}