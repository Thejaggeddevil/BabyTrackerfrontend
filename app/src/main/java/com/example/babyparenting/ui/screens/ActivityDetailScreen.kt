package com.example.babyparenting.ui.screens.millionaire

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel
import com.example.babyparenting.ui.theme.LocalAppColors

@Composable
fun ActivityDetailScreen(
    activity: DailyActivityResponse,
    isCompleted: Boolean,
    viewModel: MillionaireViewModel,
    onBackClick: () -> Unit,
    onCompleted: () -> Unit
) {
    val completionState by viewModel.completionState.collectAsState()
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        // Header with Back Button
        TopAppBar(
            title = {
                Text(
                    text = activity.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Level Badge
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LevelBadge(level = activity.level, colors = colors)
                    if (isCompleted) {
                        CompletedBadge(colors = colors)
                    }
                }
            }

            // PLAN Section
            item {
                ActivitySection(
                    title = "🎯 PLAN",
                    content = activity.plan,
                    colors = colors
                )
            }

            // DO Section
            item {
                ActivitySection(
                    title = "🚀 DO",
                    content = activity.do_instruction,
                    colors = colors
                )
            }

            // REVIEW Section
            item {
                ActivitySection(
                    title = "🤔 REVIEW",
                    content = activity.review,
                    colors = colors
                )
            }

            // REPEAT Section
            item {
                ActivitySection(
                    title = "🔁 REPEAT",
                    content = "Practice this activity regularly to build strong thinking skills. You can repeat this activity daily or whenever you have time!",
                    colors = colors,
                    isHighlighted = true
                )
            }

            // Message
            item {
                if (completionState is CompletionUiState.Success) {
                    SuccessMessage(
                        message = (completionState as CompletionUiState.Success).message,
                        colors = colors
                    )
                } else if (completionState is CompletionUiState.Error) {
                    ErrorMessage(
                        message = (completionState as CompletionUiState.Error).message,
                        colors = colors
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Action Buttons (Sticky)
        ActionButtons(
            isCompleted = isCompleted,
            isLoading = completionState is CompletionUiState.Idle || completionState is CompletionUiState.Success,
            colors = colors,
            onMarkCompleted = {
                viewModel.markActivityAsCompleted(activity.activity_id)
                onCompleted()
            }
        )
    }
}

@Composable
private fun LevelBadge(
    level: Int,
    colors: com.example.babyparenting.ui.theme.AppColors
) {
    Surface(
        color = colors.lavender.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Level $level",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CompletedBadge(
    colors: com.example.babyparenting.ui.theme.AppColors
) {
    Surface(
        color = colors.primary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = colors.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Completed",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun ActivitySection(
    title: String,
    content: String,
    colors: com.example.babyparenting.ui.theme.AppColors,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                colors.peach.copy(alpha = 0.15f) else colors.bgSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Text(
                text = content,
                fontSize = 13.sp,
                color = colors.textPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SuccessMessage(
    message: String,
    colors: com.example.babyparenting.ui.theme.AppColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = colors.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    colors: com.example.babyparenting.ui.theme.AppColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.red.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = colors.red,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun ActionButtons(
    isCompleted: Boolean,
    isLoading: Boolean,
    colors: com.example.babyparenting.ui.theme.AppColors,
    onMarkCompleted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isCompleted) {
            Button(
                onClick = onMarkCompleted,
                enabled = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    Text(
                        text = "Mark as Completed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }
        } else {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = colors.primary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Activity Completed!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        OutlinedButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.border(
                width = 1.5.dp,
                color = colors.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
        ) {
            Text(
                text = "Try Again",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }
    }
}
