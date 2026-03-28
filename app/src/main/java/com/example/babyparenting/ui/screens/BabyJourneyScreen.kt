package com.example.babyparenting.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.babyparenting.data.model.AgeGroup
import com.example.babyparenting.data.model.JourneyProgress
import com.example.babyparenting.data.model.Milestone
import com.example.babyparenting.ui.components.FootstepsLayer
import com.example.babyparenting.ui.components.MilestoneCard
import com.example.babyparenting.ui.components.PathCanvas
import com.example.babyparenting.ui.components.SectionHeader
import com.example.babyparenting.ui.components.computeNodePositions
import com.example.babyparenting.ui.theme.LocalAppColors
import com.example.babyparenting.viewmodel.JourneyViewModel

private val SEGMENT_DP: Dp = 130.dp

@Composable
fun BabyJourneyScreen(
    viewModel: JourneyViewModel,
    onMilestoneTapped: (Milestone) -> Unit,
    onSettingsTapped: () -> Unit,
    onMillionaireTapped:()->Unit,
    onParentHubTapped: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val c              = LocalAppColors.current
    val milestones     by viewModel.visibleMilestones.collectAsState()
    val ageGroups      by viewModel.ageGroups.collectAsState()
    val progress       by viewModel.progress.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val loadError      by viewModel.loadError.collectAsState()
    val completedCount  = milestones.count { it.isCompleted }
    var showAgeDialog   by remember { mutableStateOf(false) }
    var menuExpanded    by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadDataIfNeeded()
    }
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxSize().background(c.bgMain).statusBarsPadding()
        ) {
            JourneyHeader(
                progress      = progress,
                isDark        = c.isDark,
                onEditAge     = { showAgeDialog = true },
                onMenuClicked = { menuExpanded = !menuExpanded }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ){
                when {
                    isLoading         -> LoadingView()
                    loadError != null -> ErrorView(loadError!!) { viewModel.reloadDatasets() }
                    milestones.isEmpty() -> EmptyView()
                    else -> JourneyMap(
                        milestones         = milestones,
                        ageGroups          = ageGroups,
                        completedCount     = completedCount,
                        onMilestoneTapped  = { m -> if (!viewModel.isLocked(m)) onMilestoneTapped(m) },
                        onToggleCompletion = { id -> viewModel.markComplete(id) },
                        isLocked           = { viewModel.isLocked(it) }
                    )
                }
            }
        }

        // ── Hamburger dropdown menu ──────────────────────────────────────────
        AnimatedVisibility(
            visible = menuExpanded,
            enter   = fadeIn() + slideInVertically { -it / 2 },
            exit    = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopEnd).zIndex(10f)
                .statusBarsPadding().padding(top = 56.dp, end = 12.dp)
        ) {
            HamburgerMenu(
                isDark          = c.isDark,
                onParentHub     = { menuExpanded = false; onParentHubTapped() },
                onMilestones    = { menuExpanded = false },
                onMillionaire   = { menuExpanded = false; onMillionaireTapped() },  // ✅ FIXED: Added missing callback
                onSettings      = { menuExpanded = false; onSettingsTapped() },
                onToggleTheme   = { menuExpanded = false; onToggleTheme() }
            )
        }

        if (menuExpanded) {
            Box(Modifier.fillMaxSize().zIndex(9f).clickable { menuExpanded = false })
        }
    }

    if (showAgeDialog) {
        AgeDialog(
            currentAge = progress.childAgeMonths,
            childName  = progress.childName,
            onConfirm  = { name, months ->
                viewModel.setChildName(name)
                viewModel.setChildAge(months)
                showAgeDialog = false
            },
            onDismiss = { showAgeDialog = false }
        )
    }
}

// ── Hamburger dropdown ────────────────────────────────────────────────────────

@Composable
private fun HamburgerMenu(
    isDark: Boolean,
    onParentHub: () -> Unit,
    onMilestones: () -> Unit,
    onMillionaire: () -> Unit,
    onSettings: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val c = LocalAppColors.current

    Column(
        modifier = Modifier
            .width(220.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(c.menuBg)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        MenuItem(icon = Icons.Default.AdminPanelSettings, label = "Parent Hub",  color = c.lavender, onClick = onParentHub)
        MenuDivider()
        MenuItem(icon = Icons.Default.Stars,              label = "Millionaire Baby Club",   color = c.coral,    onClick = onMillionaire)
        MenuDivider()
        MenuItem(icon = Icons.Default.Person,             label = "Settings",     color = c.sky,      onClick = onSettings)
        MenuDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(c.menuItemBg).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null, tint = c.gold, modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isDark) "Dark Mode" else "Light Mode",
                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isDark, onCheckedChange = { onToggleTheme() },
                colors  = SwitchDefaults.colors(
                    checkedThumbColor   = c.bgMain, checkedTrackColor   = c.lavender,
                    uncheckedThumbColor = c.bgMain, uncheckedTrackColor = c.coral
                )
            )
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(c.menuItemBg).clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.15f))
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(17.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.textPrimary)
    }
}

@Composable
private fun MenuDivider() {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.menuDivider))
}

// ── Journey Map ───────────────────────────────────────────────────────────────


@Composable
private fun JourneyMap(
    milestones: List<Milestone>,
    ageGroups: List<AgeGroup>,
    completedCount: Int,
    onMilestoneTapped: (Milestone) -> Unit,
    onToggleCompletion: (String) -> Unit,
    isLocked: (Milestone) -> Boolean
) {
    val density       = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp


    val visibleItems = milestones

    val totalH = SEGMENT_DP * (visibleItems.size + 2)

    val wPx = with(density) { screenWidthDp.toPx() }
    val hPx = with(density) { totalH.toPx() }

    // ✅ LIGHTWEIGHT calculation
    val nodes = remember(visibleItems.size) {
        computeNodePositions(visibleItems.size, wPx, hPx)
    }

    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(totalH)
        ) {

            // ✅ SAFE canvas (limited data)
            PathCanvas(
                nodes,
                completedCount,
                Modifier.fillMaxSize()
            )

            FootstepsLayer(
                nodes,
                completedCount,
                Modifier.fillMaxSize()
            )

            visibleItems.forEachIndexed { gIdx, milestone ->

                val isFirstInGroup =
                    gIdx == 0 || visibleItems[gIdx - 1].ageGroupId != milestone.ageGroupId

                if (isFirstInGroup) {
                    val group = ageGroups.find { it.id == milestone.ageGroupId }
                    if (group != null) {
                        val headerY = with(density) { nodes[gIdx].y.toDp() } - 50.dp

                        SectionHeader(
                            group = group,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        with(density) { 12.dp.roundToPx() },
                                        headerY.roundToPx()
                                    )
                                }
                                .widthIn(max = screenWidthDp - 24.dp)
                        )
                    }
                }

                val node    = nodes[gIdx]
                val isLeft  = gIdx % 2 == 0
                val nodeXDp = with(density) { node.x.toDp() }
                val nodeYDp = with(density) { node.y.toDp() }

                val cardWidth = 160.dp
                val cardX =
                    if (isLeft) nodeXDp + 18.dp else nodeXDp - cardWidth - 18.dp

                val locked = isLocked(milestone)

                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                cardX.roundToPx(),
                                (nodeYDp - 22.dp).roundToPx()
                            )
                        }
                        .widthIn(max = cardWidth)
                ) {
                    MilestoneCard(
                        milestone = milestone,
                        index = gIdx,
                        isLocked = locked,
                        onClick = { if (!locked) onMilestoneTapped(milestone) },
                        onToggleCompletion = {
                            if (!locked && !milestone.isCompleted) {
                                onToggleCompletion(milestone.id)
                            }
                        }
                    )
                }
            }
        }
    }
}
// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun JourneyHeader(
    progress: JourneyProgress,
    isDark: Boolean,
    onEditAge: () -> Unit,
    onMenuClicked: () -> Unit
) {
    val c       = LocalAppColors.current
    val animProg by animateFloatAsState(progress.progressFraction, spring(Spring.DampingRatioNoBouncy), label = "p")

    Column(
        Modifier.fillMaxWidth().background(c.bgSurface)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (progress.childName.isNotBlank()) "${progress.childName}'s Journey" else "Baby Growth Journey",
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.textPrimary
                )
                Text("Track every milestone with love", fontSize = 12.sp, color = c.textSecondary)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(c.coral.copy(alpha = 0.12f)).clickable { onMenuClicked() }
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = c.coral, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(c.coral.copy(alpha = 0.12f)).clickable { onEditAge() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    if (progress.childAgeMonths == 0) "Set Age ▾" else formatAge(progress.childAgeMonths) + " ▾",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.coral
                )
            }
            Spacer(Modifier.width(10.dp))
            LinearProgressIndicator(
                progress   = animProg,
                modifier   = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
                color      = c.coral, trackColor = c.border
            )
            Spacer(Modifier.width(10.dp))
            Text("${progress.completedMilestones}/${progress.totalMilestones}",
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.coral)
        }
    }
}

// ── State views ───────────────────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxWidth().padding(top = 120.dp), Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = c.coral, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(16.dp))
            Text("Loading milestones…", fontSize = 14.sp, color = c.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text("Only happens once", fontSize = 12.sp, color = c.textMuted)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    val c = LocalAppColors.current
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Could not load data", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 12.sp, color = c.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = c.coral), shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp), tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Retry", color = Color.White)
        }
    }
}

@Composable
private fun EmptyView() {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxWidth().padding(top = 80.dp), Alignment.TopCenter) {
        Text("Complete the current batch to unlock more",
            fontSize = 14.sp, color = c.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp))
    }
}

// ── Age dialog — Manual input (years + months) ────────────────────────────────

@Composable
private fun AgeDialog(
    currentAge: Int,
    childName: String,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current

    // Pre-fill from existing age
    var nameInput   by remember { mutableStateOf(childName) }
    var yearsInput  by remember { mutableStateOf(if (currentAge >= 12) (currentAge / 12).toString() else "") }
    var monthsInput by remember { mutableStateOf(if (currentAge in 1..11) currentAge.toString() else if (currentAge > 12) (currentAge % 12).toString() else "") }
    var ageError    by remember { mutableStateOf("") }

    val totalMonths by remember {
        derivedStateOf {
            val y = yearsInput.trim().toIntOrNull() ?: 0
            val m = monthsInput.trim().toIntOrNull() ?: 0
            y * 12 + m
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = c.bgElevated,
        shape            = RoundedCornerShape(18.dp),
        title = {
            Text("Child's Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name field
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text("Child's name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = c.coral, unfocusedBorderColor    = c.border,
                        focusedLabelColor       = c.coral, focusedTextColor        = c.textPrimary,
                        unfocusedTextColor      = c.textPrimary,
                        focusedContainerColor   = c.bgElevated, unfocusedContainerColor = c.bgElevated
                    )
                )

                // Age section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Child's current age", fontSize = 13.sp, color = c.textSecondary,
                        fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Years
                        OutlinedTextField(
                            value         = yearsInput,
                            onValueChange = {
                                val n = it.filter { ch -> ch.isDigit() }
                                if (n.isEmpty() || (n.toIntOrNull() ?: 0) <= 12) {
                                    yearsInput = n; ageError = ""
                                }
                            },
                            label       = { Text("Years") },
                            placeholder = { Text("0", color = c.textMuted) },
                            singleLine  = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError     = ageError.isNotEmpty(),
                            modifier    = Modifier.weight(1f),
                            shape       = RoundedCornerShape(10.dp),
                            colors      = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = c.coral, unfocusedBorderColor    = c.border,
                                focusedLabelColor       = c.coral, focusedTextColor        = c.textPrimary,
                                unfocusedTextColor      = c.textPrimary,
                                focusedContainerColor   = c.bgElevated, unfocusedContainerColor = c.bgElevated
                            )
                        )

                        // Months
                        OutlinedTextField(
                            value         = monthsInput,
                            onValueChange = {
                                val n = it.filter { ch -> ch.isDigit() }
                                if (n.isEmpty() || (n.toIntOrNull() ?: 0) <= 11) {
                                    monthsInput = n; ageError = ""
                                }
                            },
                            label       = { Text("Months") },
                            placeholder = { Text("0", color = c.textMuted) },
                            singleLine  = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError     = ageError.isNotEmpty(),
                            modifier    = Modifier.weight(1f),
                            shape       = RoundedCornerShape(10.dp),
                            colors      = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = c.coral, unfocusedBorderColor    = c.border,
                                focusedLabelColor       = c.coral, focusedTextColor        = c.textPrimary,
                                unfocusedTextColor      = c.textPrimary,
                                focusedContainerColor   = c.bgElevated, unfocusedContainerColor = c.bgElevated
                            )
                        )
                    }

                    if (ageError.isNotEmpty()) {
                        Text(ageError, fontSize = 11.sp, color = c.red)
                    }

                    // Preview
                    if (totalMonths > 0) {
                        Text(
                            "Age: ${formatAge(totalMonths)} · Past milestones will be auto-completed",
                            fontSize = 11.sp, color = c.coral
                        )
                    } else {
                        Text("Enter 0 years 0 months for a newborn", fontSize = 11.sp, color = c.textMuted)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val years  = yearsInput.trim().toIntOrNull() ?: 0
                    val months = monthsInput.trim().toIntOrNull() ?: 0
                    if (years > 12 || (years == 12 && months > 0)) {
                        ageError = "Max age is 12 years"
                    } else {
                        onConfirm(nameInput.trim(), totalMonths)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = c.coral),
                shape  = RoundedCornerShape(10.dp)
            ) { Text("Save", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) }
        }
    )
}

private fun formatAge(months: Int): String = when {
    months == 0      -> "Newborn"
    months < 12      -> "$months month${if (months == 1) "" else "s"}"
    months == 12     -> "1 year"
    months % 12 == 0 -> "${months / 12} years"
    else             -> "${months / 12} yr ${months % 12} mo"
}