package com.example.aio_prodapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aio_prodapp.data.NotificationEntity
import com.example.aio_prodapp.data.NotificationType
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme
import com.example.aio_prodapp.viewmodel.AppViewModelProvider
import com.example.aio_prodapp.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

// ---------- Look & feel helpers ----------

@Composable
private fun mint(strength: Float = 0.4f): Color =
    MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = strength)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)

private val CardContainer = Color(0xFFF2F2F2)
private val DialogLabel = Color(0xFF8A8A8A)

enum class NotificationTimeGroup(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    FEW_DAYS_AGO("Few days ago")
}

private fun groupForTimestamp(epochMs: Long): NotificationTimeGroup {
    val zone = ZoneId.systemDefault()
    val eventDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val yesterday = today.minusDays(1)

    return when {
        eventDate == today -> NotificationTimeGroup.TODAY
        eventDate == yesterday -> NotificationTimeGroup.YESTERDAY
        else -> NotificationTimeGroup.FEW_DAYS_AGO
    }
}

private fun formatNotificationTime(epochMs: Long): String {
    val zone = ZoneId.systemDefault()
    val eventDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)

    return if (eventDate == today) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMs))
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}

// ---------- Overlay Screen ----------

@Composable
fun NotificationHistoryOverlay(
    onDismiss: () -> Unit,
    onNavigateToNote: (Int) -> Unit,
    onNavigateToTask: (Int) -> Unit,
    onNavigateToFocus: () -> Unit,
    viewModel: NotificationsViewModel? = null
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current || context.applicationContext !is AioApplication
    val actualViewModel: NotificationsViewModel? = if (isPreview) {
        null
    } else if (viewModel != null) {
        viewModel
    } else {
        viewModel(factory = AppViewModelProvider.Factory)
    }

    val entries = if (isPreview || actualViewModel == null) {
        listOf(
            NotificationEntity(1, NotificationType.NOTE_REMINDER, "Weekly Review", "Reflect on what went well", noteId = 1, createdAt = System.currentTimeMillis()),
            NotificationEntity(2, NotificationType.TASK_REMINDER, "Finish Assignment", "Networking Assignment (Oct 29)", taskId = 1, createdAt = System.currentTimeMillis() - 86400000L),
            NotificationEntity(3, NotificationType.FOCUS_COMPLETE, "Focus session complete", "Deep Work Session", createdAt = System.currentTimeMillis() - 172800000L)
        )
    } else {
        actualViewModel.entries.collectAsStateWithLifecycle().value
    }

    // Mark all as read when opening overlay
    LaunchedEffect(Unit) {
        actualViewModel?.markAllRead()
    }

    var showClearConfirmation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Header Row: Back Arrow | Title ("notification") | Clear All
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "notification",
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontSize = 42.sp,
                    color = MaterialTheme.colorScheme.inversePrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )

                if (entries.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showClearConfirmation = true }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notifications yet.",
                        fontSize = 14.sp,
                        color = DialogLabel,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val groupedEntries = entries.groupBy { groupForTimestamp(it.createdAt) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    NotificationTimeGroup.entries.forEach { group ->
                        val itemsInGroup = groupedEntries[group].orEmpty()
                        if (itemsInGroup.isNotEmpty()) {
                            item {
                                Text(
                                    text = group.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(itemsInGroup, key = { it.id }) { item ->
                                NotificationItem(
                                    entry = item,
                                    onClick = {
                                        when (item.type) {
                                            NotificationType.NOTE_REMINDER -> {
                                                if (item.noteId != null) {
                                                    onNavigateToNote(item.noteId)
                                                } else {
                                                    Toast.makeText(context, "This note no longer exists.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            NotificationType.TASK_REMINDER -> {
                                                if (item.taskId != null) {
                                                    onNavigateToTask(item.taskId)
                                                } else {
                                                    Toast.makeText(context, "This task no longer exists.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            NotificationType.FOCUS_COMPLETE -> {
                                                onNavigateToFocus()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        DeleteConfirmationDialog(
            message = "Are you sure you want to clear all notifications?",
            onConfirm = {
                actualViewModel?.clearAll(
                    onResult = { success ->
                        if (!success) {
                            Toast.makeText(context, "Couldn't clear notifications.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                showClearConfirmation = false
            },
            onDismiss = { showClearConfirmation = false }
        )
    }
}

// ---------- Notification Item Row ----------

@Composable
private fun NotificationItem(
    entry: NotificationEntity,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val iconRes = when (entry.type) {
        NotificationType.NOTE_REMINDER, NotificationType.TASK_REMINDER -> R.drawable.icon_bell
        NotificationType.FOCUS_COMPLETE -> R.drawable.icon_target
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardContainer)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(mint(0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.body.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.body,
                    fontSize = 12.sp,
                    color = DialogLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = formatNotificationTime(entry.createdAt),
            fontSize = 11.sp,
            color = DialogLabel
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationHistoryOverlayPreview() {
    AIOProdAppTheme {
        NotificationHistoryOverlay(
            onDismiss = {},
            onNavigateToNote = {},
            onNavigateToTask = {},
            onNavigateToFocus = {}
        )
    }
}