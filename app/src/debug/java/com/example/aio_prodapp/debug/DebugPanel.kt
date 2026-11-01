package com.example.aio_prodapp.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aio_prodapp.AioApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Dialog colors from the design list (spec 5.2): dialogs stay light gray in dark mode on purpose.
private val DialogContainer = Color(0xFFF2F2F2)
private val DialogHeading = Color(0xFF666666)
private val DialogLabel = Color(0xFF8A8A8A)
private val DialogGreen = Color(0xFF54B192)
private val DeleteContainer = Color(0xFFF8C4C4)
private val DeleteText = Color(0xFFB03A3A)

/** DEBUG implementation of the panel (spec section 6). The release build has an empty stub. */
@Composable
fun DebugPanelDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = remember { (context.applicationContext as AioApplication).container.database }
    val scope = rememberCoroutineScope()
    val shortTimers by DebugTools.shortTimersFlow.collectAsStateWithLifecycle()

    var status by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun run(action: suspend () -> String) {
        if (busy) return
        busy = true
        scope.launch {
            status = try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Failed: ${e.message}"
            }
            busy = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = DialogContainer
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Debug panel", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DialogHeading)
                Text("Debug builds only", fontSize = 14.sp, color = DialogLabel)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Short timers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DialogHeading)
                        Text("Focus 10 s, Short 5 s, Long 8 s", fontSize = 13.sp, color = DialogLabel)
                    }
                    Switch(
                        checked = shortTimers,
                        onCheckedChange = { DebugTools.setShortTimers(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = DialogGreen)
                    )
                }

                DebugButton("Fire test note reminder", enabled = !busy) {
                    confirmClear = false
                    run { DebugData.fireTestNoteReminder(context, db) }
                }
                DebugButton("Fire test task reminder", enabled = !busy) {
                    confirmClear = false
                    run { DebugData.fireTestTaskReminder(context, db) }
                }
                DebugButton("Seed sample data", enabled = !busy) {
                    confirmClear = false
                    run { DebugData.seed(db); "Sample data added: 3 notes, 4 tasks, 3-day streak." }
                }
                DebugButton(
                    text = if (confirmClear) "Tap again to erase everything" else "Clear all data",
                    container = DeleteContainer,
                    content = DeleteText,
                    enabled = !busy
                ) {
                    if (!confirmClear) {
                        confirmClear = true
                    } else {
                        confirmClear = false
                        run { DebugData.clear(db); "All data cleared." }
                    }
                }

                if (status.isNotEmpty()) {
                    Text(status, fontSize = 14.sp, color = DialogHeading)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = DialogGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun DebugButton(
    text: String,
    container: Color = DialogGreen,
    content: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
