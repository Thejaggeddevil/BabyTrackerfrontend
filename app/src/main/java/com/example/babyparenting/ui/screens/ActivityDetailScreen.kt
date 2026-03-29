package com.example.babyparenting.ui.screens.millionaire


import com.example.babyparenting.data.local.UserManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.ui.viewmodel.MillionaireViewModel
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.ui.theme.AppColorScheme
import com.example.babyparenting.ui.viewmodel.ActivityDetailUiState
import com.example.babyparenting.ui.viewmodel.CompletionUiState

// ──────────────────────────────────────────────────────────────────────────────
// ActivityDetailScreenFixed
//
// ✅ Shows:
//    - Full activity details from backend JSON
//    - Parent guidance with examples
//    - Help section with indicators
//    - Mark as Complete button
//    - Completion status tracking
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreenFixed(
    activityId: Int,
    strategyId: Int,
    viewModel: MillionaireViewModel,
    onBackClick: () -> Unit,
    onCompleted: () -> Unit = {}
) {
    val activityDetailState by viewModel.activityDetailState.collectAsState()
    val completionState by viewModel.completionState.collectAsState()
    val completedActivities by viewModel.completedActivities.collectAsState()
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val userId = UserManager.getUserId(context)
    // Load activity detail when screen appears
    LaunchedEffect(activityId) {
        viewModel.loadActivityDetail(activityId)
    }


    LaunchedEffect(Unit) {
        snapshotFlow { completionState }
            .collect { state ->
                if (state is CompletionUiState.Success) {
                    onCompleted()
                }
            }
    }

    val isAlreadyCompleted = completedActivities.contains(activityId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                when (val state = activityDetailState) {
                    is ActivityDetailUiState.Success -> {
                        Text(
                            text = state.activity.title ?: "Activity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                    else -> {
                        Text(
                            text = "Activity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }
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
            )
        )

        // Content
        when (val state = activityDetailState) {
            is ActivityDetailUiState.Success -> {
                ActivityDetailContent(
                    activity = state.activity,
                    colors = colors,
                    completionState = completionState,
                    isAlreadyCompleted = isAlreadyCompleted,
                    strategyId = strategyId,
                    onMarkCompleted = {
                        viewModel.markActivityCompleted(
                            userId = userId,
                            activityId = activityId,
                            strategyId = strategyId
                        )
                    }
                )
            }

            is ActivityDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.coral)
                }
            }

            is ActivityDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = colors.red,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityDetailContent(
    activity: com.example.babyparenting.data.model.Activity,
    colors: AppColorScheme,
    completionState: CompletionUiState,
    isAlreadyCompleted: Boolean,
    strategyId: Int,
    onMarkCompleted: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // ────────────────────────────────────────────────────────────────
            // COMPLETION STATUS BANNER
            // ────────────────────────────────────────────────────────────────
            if (isAlreadyCompleted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.coral.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = colors.coral,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "✓ You've completed this activity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.coral
                            )
                        }
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 1. META INFO (Materials, Time, Difficulty)
            // ────────────────────────────────────────────────────────────────
            activity.meta?.let { meta ->
                val hasValidData =
                    !meta.materials.isNullOrEmpty() ||
                            meta.timeMinutes != null ||
                            !meta.difficulty.isNullOrBlank()

                if (hasValidData) {
                    item {
                        MetaInfoSection(
                            materials = meta.materials ?: emptyList(),
                            timeMinutes = meta.timeMinutes ?: 30,
                            difficulty = meta.difficulty ?: "Medium",
                            colors = colors
                        )
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 2. BASIC INFO (PLAN, DO, REVIEW)
            // ────────────────────────────────────────────────────────────────
            activity.basic?.let { basic ->
                val hasValidData = !basic.plan.isNullOrBlank() || !basic.do_.isNullOrBlank() || !basic.review.isNullOrBlank()
                if (hasValidData) {
                    item {
                        BasicInfoSection(
                            plan = basic.plan,
                            do_ = basic.do_,
                            review = basic.review,
                            colors = colors
                        )
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 3. PARENT GUIDANCE
            // ────────────────────────────────────────────────────────────────
            activity.parentGuidance?.let { guidance ->
                val hasValidData = !guidance.setup.isNullOrBlank() ||
                        !guidance.planQuestions.isNullOrEmpty() ||
                        !guidance.examples.isNullOrEmpty() ||
                        !guidance.reviewPrompts.isNullOrEmpty()

                if (hasValidData) {
                    item {
                        ParentGuidanceSection(
                            setup = guidance.setup,
                            planQuestions = guidance.planQuestions ?: emptyList(),
                            examples = guidance.examples ?: emptyList(),
                            reviewPrompts = guidance.reviewPrompts ?: emptyList(),
                            colors = colors
                        )
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 4. HELP SECTION
            // ────────────────────────────────────────────────────────────────
            activity.help?.let { help ->
                val hasValidData = !help.indicators.isNullOrEmpty() ||
                        !help.mistakes.isNullOrEmpty() ||
                        !help.examples.isNullOrEmpty()

                if (hasValidData) {
                    item {
                        HelpSection(
                            indicators = help.indicators ?: emptyList(),
                            mistakes = help.mistakes ?: emptyList(),
                            examples = help.examples ?: emptyList(),
                            colors = colors
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ────────────────────────────────────────────────────────────────
        // FLOATING COMPLETION BUTTON
        // ────────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.bgMain.copy(alpha = 0f),
                            colors.bgMain
                        )
                    )
                )
                .padding(16.dp)
        ) {
            when (completionState) {
                is CompletionUiState.Loading -> {
                    // Loading state
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = colors.coral.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colors.bgMain,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Saving...", fontWeight = FontWeight.SemiBold)
                    }
                }

                is CompletionUiState.Success -> {
                    // Success state
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = colors.coral
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            completionState.message,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is CompletionUiState.Error -> {
                    // Error state
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.red.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚠️ ${completionState.message}",
                                fontSize = 12.sp,
                                color = colors.red,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Button(
                            onClick = onMarkCompleted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.coral
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "🔄 Retry",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                else -> {
                    // Idle state
                    if (!isAlreadyCompleted) {
                        Button(
                            onClick = onMarkCompleted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.coral
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "✓ Mark as Complete",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaInfoSection(
    materials: List<String>,
    timeMinutes: Int,
    difficulty: String,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📋 Activity Info",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoBadge("⏱️ $timeMinutes min", colors)
                InfoBadge("📊 $difficulty", colors)
            }

            if (materials.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🎨 Materials:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    materials.forEach { material ->
                        Text(
                            text = "• $material",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicInfoSection(
    plan: String?,
    do_: String?,
    review: String?,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📚 Activity Steps",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            plan?.let {
                InfoStep("Plan", plan, colors)
            }
            do_?.let {
                InfoStep("Do", do_, colors)
            }
            review?.let {
                InfoStep("Review", review, colors)
            }
        }
    }
}

@Composable
private fun ParentGuidanceSection(
    setup: String?,
    planQuestions: List<String>,
    examples: List<String>,
    reviewPrompts: List<String>,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "👨‍👩‍👧 Parent Guide",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            setup?.let {
                Text(
                    text = "📝 Setup: $it",
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
            }

            if (planQuestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "❓ Questions to Ask:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    planQuestions.forEach { q ->
                        Text(
                            text = "• $q",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (examples.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "💡 Examples:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    examples.forEach { ex ->
                        Text(
                            text = "• $ex",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (reviewPrompts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🔄 Review Prompts:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    reviewPrompts.forEach { rp ->
                        Text(
                            text = "• $rp",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpSection(
    indicators: List<String>,
    mistakes: List<String>,
    examples: List<String>,
    colors: AppColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🆘 Help & Tips",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            if (indicators.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "✓ Signs of Success:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.coral
                    )
                    indicators.forEach { ind ->
                        Text(
                            text = "• $ind",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (mistakes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⚠️ Common Mistakes:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    mistakes.forEach { m ->
                        Text(
                            text = "• $m",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (examples.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📸 Examples:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    examples.forEach { ex ->
                        Text(
                            text = "• $ex",
                            fontSize = 11.sp,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String, colors: AppColorScheme) {
    Surface(
        color = colors.coral.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.coral,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun InfoStep(label: String, content: String, colors: AppColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.coral
        )
        Text(
            text = content,
            fontSize = 11.sp,
            color = colors.textPrimary
        )
    }
}