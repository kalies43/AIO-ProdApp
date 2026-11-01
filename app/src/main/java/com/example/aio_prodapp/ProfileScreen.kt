package com.example.aio_prodapp

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme
import com.example.aio_prodapp.viewmodel.AppViewModelProvider
import com.example.aio_prodapp.viewmodel.ProfileUiState
import com.example.aio_prodapp.viewmodel.ProfileViewModel

// ---------- Look & feel helpers ----------

@Composable
private fun mint(strength: Float = 0.4f): Color =
    MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = strength)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)

private fun softShadow(alpha: Float = 0.2f) = Shadow(
    radius = 6.dp,
    spread = 0.dp,
    color = Color.Black,
    offset = DpOffset(x = 0.dp, y = 4.dp),
    alpha = alpha
)

private val CardContainer = Color(0xFFF2F2F2)
private val DialogContainer = Color(0xFFF2F2F2)
private val DialogHeading = Color(0xFF666666)
private val DialogLabel = Color(0xFF8A8A8A)
private val DialogButton = Color(0xFF55B091)
private val DarkGreen = Color(0xFF54B192)

// ---------- Screen ----------

@Composable
fun ProfileScreen(
    onOpenDebugPanel: () -> Unit = {},
    viewModel: ProfileViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current || context.applicationContext !is AioApplication
    val actualViewModel: ProfileViewModel? = if (isPreview) {
        null
    } else if (viewModel != null) {
        viewModel
    } else {
        viewModel(factory = AppViewModelProvider.Factory)
    }

    val uiState = if (isPreview || actualViewModel == null) {
        ProfileUiState(
            displayName = "Liz-bert Puyawan",
            initials = "LP",
            notesCount = 3,
            sessionsCount = 6,
            tasksDoneCount = 4,
            weekCounts = listOf(1, 2, 4, 3, 5, 2, 0),
            todayIndex = 3,
            streak = 3
        )
    } else {
        actualViewModel.uiState.collectAsStateWithLifecycle().value
    }

    var selectedHeaderTab by remember { mutableIntStateOf(0) } // 0 = Profile, 1 = Settings
    var showEditNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Header Switch (Profile | Settings)
        HeaderPillSwitch(
            selectedIndex = selectedHeaderTab,
            onTabSelected = { selectedHeaderTab = it }
        )

        Spacer(Modifier.height(20.dp))

        if (selectedHeaderTab == 0) {
            // PROFILE TAB CONTENT
            ProfileTabContent(
                uiState = uiState,
                onOpenDebugPanel = onOpenDebugPanel,
                onEditNameClick = { showEditNameDialog = true }
            )
        } else {
            // SETTINGS TAB CONTENT (Feature 8: Empty placeholder in v1)
            SettingsTabContent()
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = uiState.displayName,
            onDismiss = { showEditNameDialog = false },
            onSave = { newName ->
                actualViewModel?.updateDisplayName(
                    name = newName,
                    onResult = { success ->
                        if (!success) {
                            Toast.makeText(context, "Couldn't update name.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                showEditNameDialog = false
            }
        )
    }
}

// ---------- Header Switch ----------

@Composable
private fun HeaderPillSwitch(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val tabs = listOf("Profile", "Settings")
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) Modifier
                            .dropShadow(shape = shape, shadow = softShadow(alpha = 0.2f))
                            .background(DarkGreen, shape)
                        else Modifier
                    )
                    .clip(shape)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- Profile Tab Content ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileTabContent(
    uiState: ProfileUiState,
    onOpenDebugPanel: () -> Unit,
    onEditNameClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar (Rounded square with gradient mint fill)
        val avatarShape = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier
                .size(80.dp)
                .dropShadow(shape = avatarShape, shadow = softShadow(0.2f))
                .clip(avatarShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.inversePrimary
                        )
                    )
                )
                .combinedClickable(
                    onClick = onEditNameClick,
                    onLongClick = onOpenDebugPanel
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.initials,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(12.dp))

        // Display Name (Editable by tapping)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEditNameClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = uiState.displayName.ifBlank { "Tap to add your name" },
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (uiState.displayName.isBlank()) DialogLabel else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = R.drawable.icon_edit),
                contentDescription = "Edit name",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Three Stat Boxes (Notes, Sessions, Tasks Done)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                count = uiState.notesCount,
                label = if (uiState.notesCount == 1) "Note" else "Notes",
                modifier = Modifier.weight(1f)
            )
            StatBox(
                count = uiState.sessionsCount,
                label = if (uiState.sessionsCount == 1) "Session" else "Sessions",
                modifier = Modifier.weight(1f)
            )
            StatBox(
                count = uiState.tasksDoneCount,
                label = if (uiState.tasksDoneCount == 1) "Task Done" else "Tasks Done",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // This Week Progress Chart
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardContainer)
                .padding(20.dp)
        ) {
            Text(
                text = "This Week",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            WeekBarChart(
                weekCounts = uiState.weekCounts,
                todayIndex = uiState.todayIndex
            )
        }

        Spacer(Modifier.height(20.dp))

        // Current Streak Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(mint(0.35f))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Streak",
                    fontSize = 12.sp,
                    color = DialogLabel
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${uiState.streak} ${if (uiState.streak == 1) "day" else "days"}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (uiState.streak == 0) "Complete a task or focus session to start your streak."
                    else "Keep it up! You're building a great habit!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Flame icon shown ONLY when streak >= 3
            if (uiState.showFlame) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    painter = painterResource(id = R.drawable.icon_flame),
                    contentDescription = "Flame Streak",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

// ---------- Stat Box ----------

@Composable
private fun StatBox(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardContainer)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = DialogLabel,
            textAlign = TextAlign.Center
        )
    }
}

// ---------- Weekly Bar Chart ----------

@Composable
private fun WeekBarChart(
    weekCounts: List<Int>,
    todayIndex: Int
) {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxCount = maxOf(1, weekCounts.maxOrNull() ?: 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dayNames.forEachIndexed { i, day ->
            val count = weekCounts.getOrElse(i) { 0 }
            val fraction = if (maxCount > 0 && count > 0) {
                (count.toFloat() / maxCount.toFloat()).coerceIn(0.12f, 1f)
            } else {
                0.08f // stub for 0
            }
            val isToday = i == todayIndex
            val barColor = if (isToday) DarkGreen else mint(0.6f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .weight(fraction, fill = true)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(barColor)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = day,
                    fontSize = 10.sp,
                    color = if (isToday) DarkGreen else DialogLabel,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ---------- Settings Tab Content (Placeholder) ----------

@Composable
private fun SettingsTabContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "settings",
            fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
            fontSize = 50.sp,
            lineHeight = 50.sp,
            color = MaterialTheme.colorScheme.inversePrimary
        )

        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardContainer)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings coming soon in a future update.",
                fontSize = 14.sp,
                color = DialogLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------- Edit Name Dialog ----------

@Composable
private fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(DialogContainer)
                .padding(24.dp)
        ) {
            Text(
                text = "EDIT NAME",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DialogHeading
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Display Name",
                fontSize = 10.sp,
                color = DialogLabel,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            BasicTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.LightGray.copy(alpha = 0.4f))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DialogHeading)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DialogButton)
                        .clickable { onSave(nameInput.trim()) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AIOProdAppTheme {
        ProfileScreen()
    }
}