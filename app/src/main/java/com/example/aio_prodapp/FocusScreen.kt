package com.example.aio_prodapp

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aio_prodapp.debug.DebugTools
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme
import com.example.aio_prodapp.viewmodel.AppViewModelProvider
import com.example.aio_prodapp.viewmodel.FocusViewModel
import java.util.Locale
import kotlinx.coroutines.delay

// ---------- Enums & Constants ----------

enum class FocusMode(val label: String) {
    FOCUS("Focus"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

private fun getModeDurationSeconds(mode: FocusMode, shortTimers: Boolean): Long =
    if (shortTimers) {
        when (mode) {
            FocusMode.FOCUS -> 10L
            FocusMode.SHORT_BREAK -> 5L
            FocusMode.LONG_BREAK -> 8L
        }
    } else {
        when (mode) {
            FocusMode.FOCUS -> 25L * 60L
            FocusMode.SHORT_BREAK -> 5L * 60L
            FocusMode.LONG_BREAK -> 15L * 60L
        }
    }

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
private val GreenButton = Color(0xFF54B192)
private val ResetContainer = Color(0xFFE0E0E0)

// ---------- Screen ----------

@Composable
fun FocusScreen(
    viewModel: FocusViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current || context.applicationContext !is AioApplication
    val actualViewModel: FocusViewModel? = if (isPreview) {
        null
    } else if (viewModel != null) {
        viewModel
    } else {
        viewModel(factory = AppViewModelProvider.Factory)
    }

    val sessionsTodayCount = if (isPreview || actualViewModel == null) {
        0
    } else {
        actualViewModel.sessionsToday.collectAsStateWithLifecycle().value
    }

    val shortTimers by DebugTools.shortTimersFlow.collectAsStateWithLifecycle()

    var currentMode by remember { mutableStateOf(FocusMode.FOCUS) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    var totalDurationSeconds by remember(currentMode, shortTimers) {
        mutableLongStateOf(getModeDurationSeconds(currentMode, shortTimers))
    }
    var remainingSeconds by remember(currentMode, shortTimers) {
        mutableLongStateOf(getModeDurationSeconds(currentMode, shortTimers))
    }

    var currentTaskInput by remember { mutableStateOf("Deep Work Session") }
    var completedFocusCycleCount by remember { mutableIntStateOf(0) }

    // Timer Ticking Loop
    LaunchedEffect(isRunning, isPaused, currentMode, shortTimers) {
        if (isRunning && !isPaused) {
            val targetEndTimeMs = SystemClock.elapsedRealtime() + remainingSeconds * 1000L
            while (isRunning && !isPaused) {
                delay(200L)
                val nowMs = SystemClock.elapsedRealtime()
                val leftSec = maxOf(0L, (targetEndTimeMs - nowMs + 999L) / 1000L)
                remainingSeconds = leftSec

                if (leftSec <= 0L) {
                    // Timer Finished
                    isRunning = false
                    isPaused = false

                    if (currentMode == FocusMode.FOCUS) {
                        completedFocusCycleCount++
                        actualViewModel?.recordFocusCompleted(
                            currentTask = currentTaskInput,
                            onResult = { success ->
                                if (!success) {
                                    Toast.makeText(context, "Couldn't log session.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // Auto-select break mode (4th session = Long Break, otherwise Short Break)
                        currentMode = if (completedFocusCycleCount % 4 == 0) {
                            FocusMode.LONG_BREAK
                        } else {
                            FocusMode.SHORT_BREAK
                        }
                    } else {
                        // Break finished -> auto-select Focus mode
                        currentMode = FocusMode.FOCUS
                    }

                    totalDurationSeconds = getModeDurationSeconds(currentMode, shortTimers)
                    remainingSeconds = getModeDurationSeconds(currentMode, shortTimers)
                    break
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // Header
        Text(
            text = "focus mode",
            fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
            fontSize = 50.sp,
            lineHeight = 50.sp,
            color = MaterialTheme.colorScheme.inversePrimary
        )
        Text(
            text = "$sessionsTodayCount sessions completed today",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(20.dp))

        // Three-Segment Selector (Focus / Short Break / Long Break)
        ModeSegmentedSelector(
            selectedMode = currentMode,
            enabled = !isRunning,
            onModeSelected = { selected ->
                currentMode = selected
                isRunning = false
                isPaused = false
                totalDurationSeconds = getModeDurationSeconds(selected, shortTimers)
                remainingSeconds = getModeDurationSeconds(selected, shortTimers)
            }
        )

        Spacer(Modifier.height(28.dp))

        // Circular Timer Display
        CircularTimerRing(
            mode = currentMode,
            remainingSeconds = remainingSeconds,
            totalSeconds = totalDurationSeconds,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(28.dp))

        // Current Task Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardContainer)
                .padding(20.dp)
        ) {
            Text(
                text = "Current Task",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            BasicTextField(
                value = currentTaskInput,
                onValueChange = { currentTaskInput = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ResetContainer)
                        .clickable {
                            isRunning = false
                            isPaused = false
                            remainingSeconds = getModeDurationSeconds(currentMode, shortTimers)
                            totalDurationSeconds = getModeDurationSeconds(currentMode, shortTimers)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_reset),
                        contentDescription = "Reset Timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Start / Pause / Resume Button
                val buttonText = when {
                    !isRunning -> "Start"
                    isPaused -> "Resume"
                    else -> "Pause"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenButton)
                        .clickable {
                            when {
                                !isRunning -> {
                                    isRunning = true
                                    isPaused = false
                                }
                                isPaused -> {
                                    isPaused = false
                                }
                                else -> {
                                    isPaused = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tip Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(mint(0.3f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tip: Turn off notification and find quiet spot.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---------- Segmented Mode Selector ----------

@Composable
private fun ModeSegmentedSelector(
    selectedMode: FocusMode,
    enabled: Boolean,
    onModeSelected: (FocusMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(CardContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FocusMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val shape = RoundedCornerShape(50)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) Modifier
                            .dropShadow(shape = shape, shadow = softShadow(alpha = 0.2f))
                            .background(GreenButton, shape)
                        else Modifier
                    )
                    .clip(shape)
                    .then(if (enabled) Modifier.clickable { onModeSelected(mode) } else Modifier)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- Circular Timer Ring ----------

@Composable
private fun CircularTimerRing(
    mode: FocusMode,
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 1f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d : %02d", minutes, seconds)

    val ringColor = GreenButton
    val trackColor = ringColor.copy(alpha = 0.2f)

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            // Track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth)
            )
            // Progress Arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = mode.label.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = formattedTime,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Text(text = "mins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "seconds", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FocusScreenPreview() {
    AIOProdAppTheme {
        FocusScreen()
    }
}