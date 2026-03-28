package com.example.babyparenting.ui.screens.millionaire

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.data.model.Strategy
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.ui.theme.AppColorScheme
import com.example.babyparenting.ui.viewmodel.CompletionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activity: Strategy,
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
        TopAppBar(
            title = {
                Text(
                    text = activity.activity?.title ?: "Activity",
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Meta info: Time and materials
            activity.activity?.meta?.let { meta ->
                item {
                    MetaInfoRow(
                        materials = meta.materials ?: emptyList(),
                        timeMinutes = meta.timeMinutes ?: 5,
                        colors = colors
                    )
                }
            }

            // Setup guidance
            activity.activity?.parentGuidance?.setup?.let { setupGuidance ->
                if (setupGuidance.isNotEmpty()) {
                    item {
                        ParentGuidanceSection(
                            title = "📌 SETUP",
                            content = setupGuidance,
                            backgroundColor = colors.lavender.copy(alpha = 0.1f),
                            colors = colors
                        )
                    }
                }
            }

            // Plan phase
            activity.activity?.basic?.plan?.let { plan ->
                if (plan.isNotEmpty()) {
                    item {
                        ActivityPhaseSection(
                            phaseTitle = "🎯 PLAN",
                            childAction = plan,
                            colors = colors
                        )
                    }
                }
            }

            // Plan questions
            activity.activity?.parentGuidance?.planQuestions?.let { questions ->
                if (questions.isNotEmpty()) {
                    item {
                        ParentQuestionsSection(
                            title = "💬 Questions to Ask",
                            questions = questions,
                            colors = colors
                        )
                    }
                }
            }

            // Do phase
            activity.activity?.basic?.do_?.let { doPhase ->
                if (doPhase.isNotEmpty()) {
                    item {
                        ActivityPhaseSection(
                            phaseTitle = "🚀 DO",
                            childAction = doPhase,
                            colors = colors
                        )
                    }
                }
            }

            // Do guidance
            activity.activity?.parentGuidance?.do_?.let { doGuidance ->
                if (doGuidance.isNotEmpty()) {
                    item {
                        ParentGuidanceSection(
                            title = "👨‍🏫 Your Role During DO",
                            content = doGuidance,
                            backgroundColor = colors.peach.copy(alpha = 0.1f),
                            colors = colors
                        )
                    }
                }
            }

            // Review phase
            activity.activity?.basic?.review?.let { review ->
                if (review.isNotEmpty()) {
                    item {
                        ActivityPhaseSection(
                            phaseTitle = "🤔 REVIEW",
                            childAction = review,
                            colors = colors
                        )
                    }
                }
            }

            // Review prompts
            activity.activity?.parentGuidance?.reviewPrompts?.let { prompts ->
                if (prompts.isNotEmpty()) {
                    item {
                        ParentQuestionsSection(
                            title = "🔍 Reflection Questions",
                            questions = prompts,
                            colors = colors
                        )
                    }
                }
            }

            // Repeat guidance
            activity.activity?.parentGuidance?.repeat?.let { repeatGuidance ->
                if (repeatGuidance.isNotEmpty()) {
                    item {
                        ActivityPhaseSection(
                            phaseTitle = "🔁 REPEAT",
                            childAction = repeatGuidance,
                            colors = colors,
                            isHighlighted = true
                        )
                    }
                }
            }

            // Success indicators
            activity.activity?.help?.successIndicators?.let { indicators ->
                if (indicators.isNotEmpty()) {
                    item {
                        SuccessIndicatorsSection(
                            indicators = indicators,
                            colors = colors
                        )
                    }
                }
            }

            // Common mistakes
            activity.activity?.help?.commonMistakes?.let { mistakes ->
                if (mistakes.isNotEmpty()) {
                    item {
                        CommonMistakesSection(
                            mistakes = mistakes,
                            colors = colors
                        )
                    }
                }
            }

            // Example dialogue
            activity.activity?.help?.exampleDialogue?.let { dialogue ->
                if (dialogue.isNotEmpty()) {
                    item {
                        ExampleDialogueSection(
                            dialogue = dialogue,
                            colors = colors
                        )
                    }
                }
            }

            item {
                when (completionState) {
                    is CompletionUiState.Success -> {
                        SuccessMessage(
                            message = (completionState as CompletionUiState.Success).message,
                            colors = colors
                        )
                    }
                    is CompletionUiState.Error -> {
                        ErrorMessage(
                            message = (completionState as CompletionUiState.Error).message,
                            colors = colors
                        )
                    }
                    else -> {}
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        ActionButtons(
            isCompleted = isCompleted,
            isLoading = completionState is CompletionUiState.Loading,
            colors = colors,
            onMarkCompleted = {
                val activityId = activity.activity?.id?.toInt() ?: 0
                if (activityId > 0) {
                    viewModel.markActivityAsCompleted(activityId)
                    onCompleted()
                }
            }
        )
    }
}

@Composable
private fun MetaInfoRow(
    materials: List<String>,
    timeMinutes: Int,
    colors: AppColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.lavender.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("⏱️ Time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            Text("$timeMinutes min", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        if (materials.isNotEmpty()) {
            Column {
                Text("📦 Materials", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                Text("${materials.size} items", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun ParentGuidanceSection(
    title: String,
    content: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.coral
            )
            Text(
                text = content,
                fontSize = 12.sp,
                color = colors.textPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ParentQuestionsSection(
    title: String,
    questions: List<String>,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.peach.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.coral
            )

            questions.forEach { question ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("•", fontSize = 12.sp, color = colors.coral)
                    Text(
                        text = question,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessIndicatorsSection(
    indicators: List<String>,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.coral.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "✅ Signs Your Child is Learning",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.coral
            )

            indicators.forEach { indicator ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("✓", fontSize = 12.sp, color = colors.coral, fontWeight = FontWeight.Bold)
                    Text(
                        text = indicator,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CommonMistakesSection(
    mistakes: List<String>,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.red.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️ Common Mistakes to Avoid",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.red
            )

            mistakes.forEach { mistake ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("❌", fontSize = 12.sp)
                    Text(
                        text = mistake,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleDialogueSection(
    dialogue: String,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎭 Real Parent-Child Dialogue",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.coral
            )
            Text(
                text = dialogue,
                fontSize = 11.sp,
                color = colors.textPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ActivityPhaseSection(
    phaseTitle: String,
    childAction: String,
    colors: AppColorScheme,
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
                text = phaseTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.coral
            )
            Text(
                text = childAction,
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
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.coral.copy(alpha = 0.1f)
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
                tint = colors.coral,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = colors.coral,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    colors: AppColorScheme
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
                .padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionButtons(
    isCompleted: Boolean,
    isLoading: Boolean,
    colors: AppColorScheme,
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
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.coral,
                    disabledContainerColor = colors.coral.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Mark as Completed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
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
                    disabledContainerColor = colors.coral.copy(alpha = 0.3f)
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
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = colors.coral.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = "Try Again",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.coral
            )
        }
    }
}