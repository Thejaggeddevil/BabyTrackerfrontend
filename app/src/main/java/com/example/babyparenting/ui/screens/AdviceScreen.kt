package com.example.babyparenting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.babyparenting.data.model.Milestone
import com.example.babyparenting.data.model.UiState
import com.example.babyparenting.network.model.AdviceResponse
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.viewmodel.JourneyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AdviceScreen(viewModel: JourneyViewModel, onBack: () -> Unit) {
    val adviceState       by viewModel.adviceState.collectAsState()
    val selectedMilestone by viewModel.selectedMilestone.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().background(LocalAppColors.current.bgMain)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(LocalAppColors.current.bgSurface)
                .padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = LocalAppColors.current.textSecondary)
            }
            Column(Modifier.weight(1f)) {
                Text(selectedMilestone?.title ?: "Advice", fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, color = LocalAppColors.current.textPrimary, maxLines = 1)
                if (selectedMilestone != null) {
                    Text("${selectedMilestone!!.ageRange}  ·  ${selectedMilestone!!.source.displayName}",
                        fontSize = 11.sp, color = LocalAppColors.current.textSecondary)
                }
            }
        }

        when (val state = adviceState) {
            is UiState.Loading -> LoadingView()
            is UiState.Success -> AdviceContent(state.data, selectedMilestone) {
                // ✅ FIX: Mark complete THEN navigate back
                selectedMilestone?.let {
                    viewModel.markComplete(it.id)
                    // Give a moment for state update
                    CoroutineScope(Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(500)
                        viewModel.resetAdvice()
                        onBack()
                    }
                }
            }
            is UiState.Error -> ErrorView(state.message, state.retryable) { viewModel.retryAdvice() }
            is UiState.Idle  -> IdleView()
        }
    }
}

@Composable private fun LoadingView() {
    Box(Modifier.fillMaxWidth().padding(top = 100.dp), Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = LocalAppColors.current.coral, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(14.dp))
            Text("Getting advice…", fontSize = 14.sp, color = LocalAppColors.current.textSecondary)
        }
    }
}

@Composable private fun IdleView() {
    Box(Modifier.fillMaxWidth().padding(top = 100.dp), Alignment.TopCenter) {
        Text("Select a milestone to view advice", fontSize = 14.sp,
            color = LocalAppColors.current.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable private fun ErrorView(message: String, retryable: Boolean, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, null, tint = LocalAppColors.current.red, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text("Could not load advice", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LocalAppColors.current.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 13.sp, color = LocalAppColors.current.textSecondary, textAlign = TextAlign.Center)
        if (retryable) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.coral),
                shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp)); Text("Try again", color = Color.White)
            }
        }
    }
}

@Composable
private fun AdviceContent(advice: AdviceResponse, milestone: Milestone?, onMarkDone: () -> Unit) {
    val accent = milestone?.let { Color(it.accentColor) } ?: LocalAppColors.current.coral

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero
        Box(
            modifier = Modifier.fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = accent.copy(0.30f), spotColor = accent.copy(0.30f))
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.70f))))
                .padding(20.dp)
        ) {
            Column {
                if (advice.domain.isNotBlank()) {
                    Text(advice.domain.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = Color.White.copy(0.75f), letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Text(advice.title.ifBlank { milestone?.title ?: "" },
                    fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 26.sp)
            }
        }

        if (advice.goal.isNotBlank())   AdviceCard("Goal",           advice.goal,    LocalAppColors.current.sky)
        if (advice.why.isNotBlank())    AdviceCard("Why it Matters",  advice.why,     LocalAppColors.current.gold)
        if (advice.how.isNotBlank())    AdviceCard("How to Do It",    advice.how,     LocalAppColors.current.mint)
        if (advice.steps.isNotEmpty())  StepsCard(advice.steps)

        if (advice.dos.isNotEmpty() || advice.donts.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (advice.dos.isNotEmpty())   BulletCard("Do",    advice.dos,   LocalAppColors.current.mint,  Modifier.weight(1f))
                if (advice.donts.isNotEmpty()) BulletCard("Avoid", advice.donts, LocalAppColors.current.red,   Modifier.weight(1f))
            }
        }

        if (advice.tip.isNotBlank())     TipCard(advice.tip)
        if (advice.example.isNotBlank()) AdviceCard("Example", advice.example, LocalAppColors.current.lavender)
        if (advice.answer.isNotBlank())  AdviceCard("Answer",  advice.answer,  LocalAppColors.current.mint)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick  = onMarkDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.mint)
        ) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp), tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text("Mark as Complete", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable private fun AdviceCard(label: String, body: String, labelColor: Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalAppColors.current.bgSurface)
        .border(1.dp, labelColor.copy(0.25f), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 14.sp, color = LocalAppColors.current.textPrimary, lineHeight = 21.sp)
    }
}

@Composable private fun StepsCard(steps: List<String>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LocalAppColors.current.bgSurface)
        .border(1.dp, LocalAppColors.current.sky.copy(0.25f), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text("Steps", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LocalAppColors.current.sky, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(10.dp))
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 9.dp)) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(LocalAppColors.current.coral), Alignment.Center) {
                    Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text(step, fontSize = 13.sp, color = LocalAppColors.current.textPrimary, lineHeight = 19.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun BulletCard(label: String, items: List<String>, labelColor: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(LocalAppColors.current.bgSurface)
        .border(1.dp, labelColor.copy(0.25f), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(7.dp))
        items.take(5).forEach { Text("• $it", fontSize = 12.sp, color = LocalAppColors.current.textPrimary,
            lineHeight = 17.sp, modifier = Modifier.padding(bottom = 4.dp)) }
        if (items.size > 5) Text("+${items.size - 5} more", fontSize = 11.sp, color = LocalAppColors.current.textMuted)
    }
}

@Composable private fun TipCard(tip: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(LocalAppColors.current.gold.copy(alpha = 0.10f))
        .border(1.dp, LocalAppColors.current.yellow.copy(0.30f), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text("Quick Tip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LocalAppColors.current.gold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(5.dp))
        Text(tip, fontSize = 13.sp, color = LocalAppColors.current.textPrimary, lineHeight = 19.sp)
    }
}