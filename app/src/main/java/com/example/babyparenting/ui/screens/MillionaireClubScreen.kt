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
import androidx.compose.ui.draw.alpha
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
                    "Millionaire Baby Club",
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
    val isCompleted  = strategy.total_activities > 0 &&
            strategy.completed_count >= strategy.total_activities
    val hasProgress  = strategy.completed_count > 0 && !isCompleted
    val isLocked     = strategy.is_locked
    val isLeft       = index % 2 == 0

    // Golden colors for completed state
    val goldenColor      = Color(0xFFFFB830)
    val goldenLightColor = Color(0xFFFFF3D0)

    // 3-column: [left card slot] [44dp center node] [right card slot]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // LEFT slot
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isLeft) {
                StrategyCard(
                    strategy       = strategy,
                    isCompleted    = isCompleted,
                    hasProgress    = hasProgress,
                    isLocked       = isLocked,
                    colors         = colors,
                    goldenColor    = goldenColor,
                    goldenLightColor = goldenLightColor,
                    onClick        = { if (!isLocked) onClick() }
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // CENTER node + vertical connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            // Animated shimmer for completed node
            val infiniteTransition = rememberInfiniteTransition()
            val shimmerAlpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isLocked    -> colors.bgSurface
                            isCompleted -> goldenColor.copy(alpha = shimmerAlpha)  // ✨ Golden animated
                            hasProgress -> colors.coral.copy(alpha = 0.25f)
                            else        -> colors.bgSurface
                        }
                    )
                    .border(
                        width = if (isLocked) 1.5.dp else 2.5.dp,
                        color = when {
                            isLocked    -> colors.textPrimary.copy(alpha = 0.15f)
                            isCompleted -> goldenColor                              // ✨ Golden border
                            hasProgress -> colors.coral.copy(alpha = 0.6f)
                            else        -> colors.coral.copy(alpha = 0.35f)
                        },
                        shape = CircleShape
                    )
            ) {
                when {
                    isLocked -> Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.textPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                    isCompleted -> Text(
                        "✓",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    else -> Text(
                        text = "${index + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasProgress) colors.coral
                        else colors.coral.copy(alpha = 0.7f)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (strategy.description.length > 60) 90.dp else 70.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isCompleted) goldenColor.copy(alpha = 0.6f)
                                    else colors.coral.copy(alpha = 0.25f),
                                    colors.coral.copy(alpha = 0.06f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // RIGHT slot
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (!isLeft) {
                StrategyCard(
                    strategy       = strategy,
                    isCompleted    = isCompleted,
                    hasProgress    = hasProgress,
                    isLocked       = isLocked,
                    colors         = colors,
                    goldenColor    = goldenColor,
                    goldenLightColor = goldenLightColor,
                    onClick        = { if (!isLocked) onClick() }
                )
            }
        }
    }
}

@Composable
private fun StrategyCard(
    strategy: Strategy,
    isCompleted: Boolean,
    hasProgress: Boolean,
    isLocked: Boolean,
    colors: AppColorScheme,
    goldenColor: Color,
    goldenLightColor: Color,
    onClick: () -> Unit
) {
    val cardAlpha = if (isLocked) 0.45f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isLocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isLocked    -> colors.bgSurface
                isCompleted -> goldenLightColor           // ✨ Warm golden cream
                hasProgress -> colors.bgSurface
                else        -> colors.bgSurface
            }
        ),
        elevation = CardDefaults.cardElevation(if (isLocked) 0.dp else if (isCompleted) 4.dp else 2.dp),
        border = when {
            isLocked    -> BorderStroke(1.dp, colors.textPrimary.copy(alpha = 0.1f))
            isCompleted -> BorderStroke(2.dp, goldenColor.copy(alpha = 0.7f))     // ✨ Golden border
            hasProgress -> BorderStroke(1.dp, colors.coral.copy(alpha = 0.25f))
            else        -> BorderStroke(1.dp, colors.textPrimary.copy(alpha = 0.08f))
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        // Completed card gets a subtle golden gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isCompleted) Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                goldenColor.copy(alpha = 0.08f),
                                goldenLightColor
                            )
                        )
                    ) else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .alpha(cardAlpha),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                // ✨ COMPLETED BADGE — shown only for completed cards
                if (isCompleted) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(goldenColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("🏆", fontSize = 9.sp)
                        Text(
                            "COMPLETED!",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB8860B)   // dark golden
                        )
                    }
                }

                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = strategy.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isLocked    -> colors.textPrimary.copy(alpha = 0.4f)
                            isCompleted -> Color(0xFF7A5C00)   // ✨ Rich golden-brown text
                            else        -> colors.textPrimary
                        },
                        maxLines = 3,
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isLocked) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isCompleted) goldenColor else colors.coral.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp).padding(top = 2.dp)
                        )
                    }
                }

                if (strategy.description.isNotBlank() && !isLocked) {
                    Text(
                        text = strategy.description,
                        fontSize = 10.sp,
                        color = if (isCompleted)
                            Color(0xFF7A5C00).copy(alpha = 0.6f)  // ✨ golden-brown description
                        else
                            colors.textPrimary.copy(alpha = 0.5f),
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }

                if (isLocked) {
                    Text(
                        "Complete previous to unlock",
                        fontSize = 9.sp,
                        color = colors.textPrimary.copy(alpha = 0.35f),
                        lineHeight = 13.sp
                    )
                } else {
                    // Progress count + age
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${strategy.completed_count}/${strategy.total_activities}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) goldenColor else colors.coral   // ✨ golden count
                        )
                        if (strategy.age_max > 0) {
                            Text(
                                "Age ${strategy.age_min}–${strategy.age_max}",
                                fontSize = 9.sp,
                                color = if (isCompleted)
                                    Color(0xFF7A5C00).copy(alpha = 0.4f)
                                else
                                    colors.textPrimary.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // Progress bar — only shown when there are real activities
                    if (strategy.total_activities > 0) {
                        LinearProgressIndicator(
                            progress = (strategy.completed_count.toFloat() / strategy.total_activities).coerceIn(0f, 1f),
                            modifier   = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color      = if (isCompleted) goldenColor else colors.coral,   // ✨ golden bar
                            trackColor = if (isCompleted)
                                goldenColor.copy(alpha = 0.2f)
                            else
                                colors.coral.copy(alpha = 0.12f)
                        )
                    }
                }
            }
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