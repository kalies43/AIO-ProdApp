package com.example.aio_prodapp

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aio_prodapp.data.NoteEntity
import com.example.aio_prodapp.viewmodel.AppViewModelProvider
import com.example.aio_prodapp.viewmodel.NotesUiState
import com.example.aio_prodapp.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------- Colors (from the Figma design) ----------

private val DialogContainer = Color(0xFFF2F2F2)
private val DialogHeading = Color(0xFF666666)
private val DialogLabel = Color(0xFF8A8A8A)
private val DialogButton = Color(0xFF55B091)
private val NoteDeleteContainer = Color(0xFFF8C4C4)
private val NoteDeleteIcon = Color(0xFFB03A3A)

private val NoteGreen = Color(0xFF54B192)     // reminder date + description text on the cards
private val AddNoteGreen = Color(0xFF6EDDB8)  // "+ Add Note" pill

@Composable
private fun noteCardColor(): Color =
    MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = 0.4f)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)

// ---------- Layout and text sizes (change these to tweak the Home screen) ----------
// Dongle draws small, so these are bigger than the numbers you would normally use.

private val ScreenPadding = 36.dp
private val CardHeight = 174.dp

private val SubtitleSize = 20.sp
private val CardReminderSize = 17.sp
private val CardTitleSize = 18.sp
private val CardBodySize = 17.sp
private val CardBodyLineHeight = 16.sp
private val CardDateSize = 16.sp

// ---------- Date helpers ----------
// The date picker works in UTC, so we format in UTC to avoid being off by one day.

private val utc: TimeZone = TimeZone.getTimeZone("UTC")

private fun reminderToMillis(text: String): Long? = try {
    if (text.isBlank()) null
    else SimpleDateFormat("MM/dd/yyyy", Locale.US).apply { timeZone = utc }.parse(text)?.time
} catch (e: Exception) {
    null
}

private fun millisToReminder(millis: Long): String =
    SimpleDateFormat("MM/dd/yyyy", Locale.US).apply { timeZone = utc }.format(Date(millis))

private fun shortDateFromMillis(millis: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).apply { timeZone = utc }.format(Date(millis))

// ---------- Screen ----------

@Composable
fun MyNotesScreen(
    pendingNoteId: Int? = null,
    onClearPendingNoteId: () -> Unit = {},
    viewModel: NotesViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current || context.applicationContext !is AioApplication
    val actualViewModel: NotesViewModel? = if (isPreview) {
        null
    } else if (viewModel != null) {
        viewModel
    } else {
        viewModel(factory = AppViewModelProvider.Factory)
    }
    val uiState = if (isPreview || actualViewModel == null) {
        NotesUiState(
            notes = listOf(
                NoteEntity(1, "Weekly Review", "Reflect on what went well, what to improve, and set intentions for next week.", reminderDate = 1790726400000L, createdAt = 1758931200000L),
                NoteEntity(2, "Book List", "Atomic Habits, Deep Work, The Power of Now, Digital Minimalism.", reminderDate = null, createdAt = 1758758400000L),
                NoteEntity(3, "Study Plan", "Finish the wireframes and review for the exam.", reminderDate = 1791158400000L, createdAt = 1758326400000L)
            ),
            noteCount = 3,
            reminderCount = 2,
            isLoaded = true
        )
    } else {
        val state by actualViewModel.uiState.collectAsStateWithLifecycle()
        state
    }

    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var deletingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Support pendingNoteId from notifications or deep links
    LaunchedEffect(pendingNoteId) {
        if (pendingNoteId != null && actualViewModel != null) {
            val note = actualViewModel.getNote(pendingNoteId)
            if (note != null) {
                editingNote = note
            } else {
                Toast.makeText(context, "This note no longer exists.", Toast.LENGTH_SHORT).show()
            }
            onClearPendingNoteId()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: title on the left, "+ Add Note" pill on the right (no calendar button in the design)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = ScreenPadding, end = ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "my notes",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontSize = 50.sp,
                lineHeight = 50.sp,
                color = MaterialTheme.colorScheme.inversePrimary,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
            Spacer(Modifier.width(12.dp))
            AddNote(onClick = { showAddDialog = true })
        }
        Text(
            text = "${uiState.noteCount} notes\n${uiState.reminderCount} reminders",
            fontSize = SubtitleSize,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = ScreenPadding + 5.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (uiState.notes.isEmpty() && uiState.isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notes yet. Tap + Add Note to create one.",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(uiState.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { editingNote = note },
                        onEdit = { editingNote = note },
                        onDelete = { deletingNote = note }
                    )
                }
            }
        }
    }

    // Edit Note dialog: shows when a note is tapped
    editingNote?.let { note ->
        NoteDialog(
            heading = "EDIT NOTE",
            saveLabel = "Save Changes",
            initialTitle = note.title,
            initialDescription = note.description,
            initialReminder = note.reminderDate?.let { millisToReminder(it) } ?: "",
            onDismiss = { editingNote = null },
            onSave = { title, description, reminder ->
                actualViewModel?.saveNote(
                    id = note.id,
                    title = title,
                    description = description,
                    reminderDate = reminderToMillis(reminder),
                    onResult = { success ->
                        if (!success) {
                            Toast.makeText(context, "Couldn't save. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                editingNote = null
            }
        )
    }

    // Add Note dialog: shows when "+ Add Note" is tapped
    if (showAddDialog) {
        NoteDialog(
            heading = "ADD NOTE",
            saveLabel = "Add Note",
            onDismiss = { showAddDialog = false },
            onSave = { title, description, reminder ->
                actualViewModel?.saveNote(
                    id = null,
                    title = title,
                    description = description,
                    reminderDate = reminderToMillis(reminder),
                    onResult = { success ->
                        if (!success) {
                            Toast.makeText(context, "Couldn't save. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                showAddDialog = false
            }
        )
    }

    // Delete Confirmation dialog
    deletingNote?.let { note ->
        DeleteConfirmationDialog(
            message = "Are you sure you want to delete?",
            onConfirm = {
                actualViewModel?.deleteNote(
                    id = note.id,
                    onResult = { success ->
                        if (!success) {
                            Toast.makeText(context, "Couldn't delete. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                deletingNote = null
            },
            onDismiss = { deletingNote = null }
        )
    }
}

// ---------- Note card ----------

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            // The shadow goes first so the clip below does not cut it off.
            // The card color is opaque, which the drop shadow needs.
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 4.dp,
                    spread = 0.dp,
                    color = Color.Black,
                    offset = DpOffset(x = 0.dp, y = 4.dp),
                    alpha = 0.25f
                )
            )
            .clip(shape)
            .background(noteCardColor(), shape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)
    ) {
        // Top row: reminder (bell + date) on the left, edit and delete on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.reminderDate != null) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_bell),
                    contentDescription = "Reminder",
                    modifier = Modifier.size(13.dp),
                    tint = NoteGreen
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = shortDateFromMillis(note.reminderDate),
                    fontSize = CardReminderSize,
                    fontWeight = FontWeight.Bold,
                    color = NoteGreen
                )
            }
            Spacer(Modifier.weight(1f))
            MiniIconButton(
                iconRes = R.drawable.icon_edit,
                description = "Edit note",
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onEdit
            )
            Spacer(Modifier.width(4.dp))
            MiniIconButton(
                iconRes = R.drawable.icon_delete,
                description = "Delete note",
                container = NoteDeleteContainer,
                tint = NoteDeleteIcon,
                onClick = onDelete
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = note.title.uppercase(),
            fontSize = CardTitleSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = note.description,
            fontSize = CardBodySize,
            lineHeight = CardBodyLineHeight,
            color = NoteGreen,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = shortDateFromMillis(note.createdAt),
            fontSize = CardDateSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun MiniIconButton(
    iconRes: Int,
    description: String,
    container: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 22.dp, height = 18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            modifier = Modifier.size(11.dp),
            tint = tint
        )
    }
}

// ---------- The Add / Edit dialog ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDialog(
    heading: String,
    saveLabel: String,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, reminder: String) -> Unit,
    initialTitle: String = "",
    initialDescription: String = "",
    initialReminder: String = ""
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var reminder by remember { mutableStateOf(initialReminder) }
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(DialogContainer)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = heading,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = DialogHeading
            )

            Spacer(Modifier.height(20.dp))

            FieldLabel("Title")
            NoteTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Description")
            NoteTextField(
                value = description,
                onValueChange = { description = it },
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel("Set Reminder")
                if (reminder.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { reminder = "" }
                            .padding(bottom = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.ifEmpty { "MM/DD/YYYY" },
                    fontSize = 12.sp,
                    color = if (reminder.isEmpty()) DialogLabel else Color.Black
                )
                Icon(
                    painter = painterResource(id = R.drawable.icon_calendar),
                    contentDescription = "Pick a date",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))

            // Save button (needs a title)
            val canSave = title.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DialogButton.copy(alpha = if (canSave) 1f else 0.4f))
                    .clickable(enabled = canSave) {
                        onSave(title.trim(), description.trim(), reminder)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = saveLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminderToMillis(reminder)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { reminder = millisToReminder(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ---------- Delete Confirmation Dialog ----------

@Composable
fun DeleteConfirmationDialog(
    message: String = "Are you sure you want to delete?",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(DialogContainer)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Yes (outlined, left)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, DialogButton, RoundedCornerShape(10.dp))
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DialogButton
                    )
                }
                // No (filled green, right)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DialogButton)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = DialogLabel,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun NoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = TextStyle(fontSize = 12.sp, color = Color.Black),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

// ---------- Add Note button (header pill) ----------

@Composable
fun AddNote(onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(AddNoteGreen, shape)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.icon_add),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Add Note",
            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}