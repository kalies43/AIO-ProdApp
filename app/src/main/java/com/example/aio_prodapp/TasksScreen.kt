package com.example.aio_prodapp

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aio_prodapp.data.SubtaskEntity
import com.example.aio_prodapp.data.TaskEntity
import com.example.aio_prodapp.data.TaskFilter
import com.example.aio_prodapp.data.TaskPriority
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme
import com.example.aio_prodapp.viewmodel.AppViewModelProvider
import com.example.aio_prodapp.viewmodel.TasksUiState
import com.example.aio_prodapp.viewmodel.TasksViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ---------- Look & feel helpers ----------

@Composable
private fun mint(strength: Float = 0.4f): Color =
    MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = strength)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)

private val DeleteContainer = Color(0xFFF8C4C4)
private val DeleteIcon = Color(0xFFB03A3A)
private val DialogContainer = Color(0xFFF2F2F2)
private val DialogHeading = Color(0xFF666666)
private val DialogLabel = Color(0xFF8A8A8A)
private val DialogButton = Color(0xFF55B091)
private val CustomGreen = Color(0xFF54B192)

private fun softShadow(alpha: Float = 0.2f) = Shadow(
    radius = 6.dp,
    spread = 0.dp,
    color = Color.Black,
    offset = DpOffset(x = 0.dp, y = 4.dp),
    alpha = alpha
)

// ---------- Date helpers (UTC) ----------

private val utc: TimeZone = TimeZone.getTimeZone("UTC")

private fun taskReminderToMillis(text: String): Long? = try {
    if (text.isBlank()) null
    else SimpleDateFormat("MM/dd/yyyy", Locale.US).apply { timeZone = utc }.parse(text)?.time
} catch (e: Exception) {
    null
}

private fun millisToTaskReminder(millis: Long): String =
    SimpleDateFormat("MM/dd/yyyy", Locale.US).apply { timeZone = utc }.format(Date(millis))

private fun formatTaskCardDate(millis: Long): String =
    SimpleDateFormat("MMMM d, yyyy", Locale.US).apply { timeZone = utc }.format(Date(millis))

// ---------- Checkbox ----------

@Composable
fun TaskCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (checked) Modifier.background(CustomGreen)
                else Modifier
                    .background(Color.White)
                    .border(1.5.dp, CustomGreen, shape)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Canvas(modifier = Modifier.size(size * 0.6f)) {
                val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val path = Path().apply {
                    moveTo(this@Canvas.size.width * 0.15f, this@Canvas.size.height * 0.45f)
                    lineTo(this@Canvas.size.width * 0.4f, this@Canvas.size.height * 0.75f)
                    lineTo(this@Canvas.size.width * 0.85f, this@Canvas.size.height * 0.2f)
                }
                drawPath(path, Color.White, style = stroke)
            }
        }
    }
}

// ---------- Screen ----------

@Composable
fun TasksScreen(
    pendingTaskId: Int? = null,
    onClearPendingTaskId: () -> Unit = {},
    viewModel: TasksViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current || context.applicationContext !is AioApplication
    val actualViewModel: TasksViewModel? = if (isPreview) {
        null
    } else if (viewModel != null) {
        viewModel
    } else {
        viewModel(factory = AppViewModelProvider.Factory)
    }

    val currentFilter = actualViewModel?.filter?.collectAsStateWithLifecycle()?.value ?: TaskFilter.ALL
    val currentQuery = actualViewModel?.query?.collectAsStateWithLifecycle()?.value ?: ""
    val uiState = if (isPreview || actualViewModel == null) {
        TasksUiState(
            tasks = listOf(
                TaskEntity(1, "Finish Assignment", "Networking Assignment (Oct 29)", reminderDate = 1788048000000L, priority = TaskPriority.TOP, isDone = false, notes = "Need to research topologies", createdAt = System.currentTimeMillis() - 10000),
                TaskEntity(2, "Workout- Legs", "Leg day at the gym", reminderDate = 1788652800000L, priority = TaskPriority.MEDIUM, isDone = false, notes = "", createdAt = System.currentTimeMillis() - 8000),
                TaskEntity(3, "Laundry", "Wash and fold clothes", reminderDate = 1788825600000L, priority = TaskPriority.LOW, isDone = false, notes = "", createdAt = System.currentTimeMillis() - 6000),
                TaskEntity(4, "Buy groceries", "Milk, Eggs, Bread", reminderDate = 1789000000000L, priority = TaskPriority.MEDIUM, isDone = true, notes = "", createdAt = System.currentTimeMillis() - 4000, completedAt = System.currentTimeMillis())
            ),
            hasAnyTasks = true,
            isLoaded = true
        )
    } else {
        actualViewModel.uiState.collectAsStateWithLifecycle().value
    }

    var selectedTaskId by remember { mutableStateOf<Int?>(1) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var deletingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Support pendingTaskId from notifications or deep links
    LaunchedEffect(pendingTaskId) {
        if (pendingTaskId != null && actualViewModel != null) {
            val task = actualViewModel.getTask(pendingTaskId)
            if (task != null) {
                editingTask = task
            } else {
                Toast.makeText(context, "This task no longer exists.", Toast.LENGTH_SHORT).show()
            }
            onClearPendingTaskId()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        // Title
        Text(
            text = "my tasks",
            fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
            fontSize = 50.sp,
            lineHeight = 50.sp,
            color = MaterialTheme.colorScheme.inversePrimary
        )
        Text(
            text = "Manage and organize your tasks",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(24.dp))

        // Search + Add Task
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TaskSearchBar(
                query = currentQuery,
                onQueryChange = { actualViewModel?.setQuery(it) },
                modifier = Modifier.weight(1f)
            )
            AddTaskButton(onClick = { showAddTaskDialog = true })
        }

        Spacer(Modifier.height(18.dp))

        // Filter chips (All / Active / Done)
        val filters = TaskFilter.entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { filterTab ->
                FilterChipItem(
                    label = when (filterTab) {
                        TaskFilter.ALL -> "All"
                        TaskFilter.ACTIVE -> "Active"
                        TaskFilter.DONE -> "Done"
                    },
                    selected = currentFilter == filterTab,
                    onClick = { actualViewModel?.setFilter(filterTab) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // List or Empty state
        if (!uiState.hasAnyTasks && uiState.isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks yet. Tap Add Task to get started.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else if (uiState.tasks.isEmpty() && uiState.isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks match.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
            ) {
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        selected = task.id == selectedTaskId,
                        onClick = {
                            selectedTaskId = task.id
                            editingTask = task
                        },
                        onToggleDone = { done ->
                            actualViewModel?.setTaskDone(
                                context = context,
                                taskId = task.id,
                                done = done,
                                onResult = { success ->
                                    if (!success) {
                                        Toast.makeText(context, "Couldn't update task.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        onEdit = { editingTask = task },
                        onDelete = { deletingTask = task }
                    )
                }
            }
        }
    }

    // Add Task dialog (create mode)
    if (showAddTaskDialog) {
        TaskDetailsDialog(
            task = null,
            viewModel = actualViewModel,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, description, reminderDate, priority, notes, newSubtasks ->
                actualViewModel?.saveTask(
                    context = context,
                    id = null,
                    title = title,
                    description = description,
                    reminderDate = reminderDate,
                    priority = priority,
                    notes = notes,
                    newSubtasks = newSubtasks,
                    onResult = { taskId ->
                        if (taskId == null) {
                            Toast.makeText(context, "Couldn't save task. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                showAddTaskDialog = false
            },
            onDelete = { showAddTaskDialog = false }
        )
    }

    // Edit / View Task Details dialog
    editingTask?.let { task ->
        TaskDetailsDialog(
            task = task,
            viewModel = actualViewModel,
            onDismiss = { editingTask = null },
            onSave = { title, description, reminderDate, priority, notes, _ ->
                actualViewModel?.saveTask(
                    context = context,
                    id = task.id,
                    title = title,
                    description = description,
                    reminderDate = reminderDate,
                    priority = priority,
                    notes = notes,
                    onResult = { savedId ->
                        if (savedId == null) {
                            Toast.makeText(context, "Couldn't save task. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                editingTask = null
            },
            onDelete = {
                editingTask = null
                deletingTask = task
            }
        )
    }

    // Delete Confirmation dialog
    deletingTask?.let { task ->
        DeleteConfirmationDialog(
            message = "Are you sure you want to delete?",
            onConfirm = {
                actualViewModel?.deleteTask(
                    context = context,
                    taskId = task.id,
                    onResult = { success ->
                        if (!success) {
                            Toast.makeText(context, "Couldn't delete task. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                deletingTask = null
            },
            onDismiss = { deletingTask = null }
        )
    }
}

// ---------- TaskItem Card ----------

@Composable
fun TaskItem(
    task: TaskEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val contentAlpha = if (selected) 1f else 0.7f
    val titleColor = if (task.isDone) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dropShadow(shape = shape, shadow = softShadow())
            .background(
                if (selected) mint() else MaterialTheme.colorScheme.surfaceContainerLowest,
                shape
            )
            .then(
                if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskCheckbox(
            checked = task.isDone,
            onCheckedChange = onToggleDone,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.reminderDate != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatTaskCardDate(task.reminderDate),
                        fontSize = 10.sp,
                        color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.alpha(contentAlpha),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TaskActionButton(
                iconRes = R.drawable.icon_edit,
                description = "Edit task",
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onEdit
            )
            TaskActionButton(
                iconRes = R.drawable.icon_delete,
                description = "Delete task",
                container = DeleteContainer,
                tint = DeleteIcon,
                onClick = onDelete
            )
        }
    }
}

@Composable
fun TaskActionButton(
    iconRes: Int,
    description: String,
    container: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .then(
                if (selected) Modifier
                    .dropShadow(shape = shape, shadow = softShadow(alpha = 0.25f))
                    .background(mint(), shape)
                else Modifier
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun TaskSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.icon_hamburg),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search task",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Icon(
            painter = painterResource(R.drawable.icon_search),
            contentDescription = "Search",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AddTaskButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .dropShadow(shape = shape, shadow = softShadow(alpha = 0.25f))
            .background(mint(), shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Add Task",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ---------- Task Details Dialog (View / Edit / Create) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsDialog(
    task: TaskEntity?,
    viewModel: TasksViewModel?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        reminderDate: Long?,
        priority: TaskPriority,
        notes: String,
        newSubtasks: List<String>
    ) -> Unit,
    onDelete: () -> Unit
) {
    val isCreateMode = task == null
    var isEditing by remember { mutableStateOf(isCreateMode) }

    var title by remember { mutableStateOf(task?.title.orEmpty()) }
    var description by remember { mutableStateOf(task?.description.orEmpty()) }
    var reminderText by remember { mutableStateOf(task?.reminderDate?.let { millisToTaskReminder(it) }.orEmpty()) }
    var priority by remember { mutableStateOf(task?.priority ?: TaskPriority.MEDIUM) }
    var notes by remember { mutableStateOf(task?.notes.orEmpty()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }

    // Subtasks for existing task (observed from Room) vs local memory list for create mode
    val existingSubtasks = if (!isCreateMode && task != null && viewModel != null) {
        viewModel.subtasks(task.id).collectAsStateWithLifecycle(initialValue = emptyList()).value
    } else {
        emptyList()
    }
    val localSubtaskTitles = remember { mutableStateListOf<String>() }
    var newSubtaskInput by remember { mutableStateOf("") }

    val subtasksDoneCount = if (isCreateMode) 0 else existingSubtasks.count { it.isDone }
    val subtasksTotalCount = if (isCreateMode) localSubtaskTitles.size else existingSubtasks.size

    fun handleAddSubtask() {
        val clean = newSubtaskInput.trim()
        if (clean.isNotEmpty()) {
            if (!isCreateMode && task != null && viewModel != null) {
                viewModel.addSubtask(task.id, clean)
            } else {
                localSubtaskTitles.add(clean)
            }
            newSubtaskInput = ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DialogContainer)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCreateMode) "ADD TASK" else "Task Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = DialogHeading
                )
                if (!isCreateMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isEditing) "Viewing" else "Edit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DialogButton,
                            modifier = Modifier
                                .clickable { isEditing = !isEditing }
                                .padding(4.dp)
                        )
                        Text(
                            text = "Delete",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeleteIcon,
                            modifier = Modifier
                                .clickable(onClick = onDelete)
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title
            DialogFieldLabel("Title")
            TaskDialogTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                readOnly = !isEditing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Description
            DialogFieldLabel("Description")
            TaskDialogTextField(
                value = description,
                onValueChange = { description = it },
                singleLine = false,
                readOnly = !isEditing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Subtasks Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogFieldLabel("Subtasks")
                if (subtasksTotalCount > 0) {
                    Text(
                        text = "$subtasksDoneCount of $subtasksTotalCount done",
                        fontSize = 10.sp,
                        color = DialogLabel
                    )
                }
            }

            if (subtasksTotalCount == 0) {
                Text(
                    text = "No subtasks yet.",
                    fontSize = 11.sp,
                    color = DialogLabel,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isCreateMode) {
                        existingSubtasks.forEach { subtask ->
                            SubtaskRowItem(
                                title = subtask.title,
                                isDone = subtask.isDone,
                                onToggleDone = { done -> viewModel?.setSubtaskDone(subtask.id, done) },
                                onDelete = { viewModel?.deleteSubtask(subtask.id) }
                            )
                        }
                    } else {
                        localSubtaskTitles.forEachIndexed { index, subtaskTitle ->
                            SubtaskRowItem(
                                title = subtaskTitle,
                                isDone = false,
                                onToggleDone = {},
                                onDelete = { localSubtaskTitles.removeAt(index) }
                            )
                        }
                    }
                }
            }

            // Input row to add a subtask
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = newSubtaskInput,
                    onValueChange = { newSubtaskInput = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.Black),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleAddSubtask() }),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) { innerTextField ->
                    if (newSubtaskInput.isEmpty()) {
                        Text("Add a subtask", fontSize = 12.sp, color = DialogLabel)
                    }
                    innerTextField()
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DialogButton)
                        .clickable { handleAddSubtask() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_add),
                        contentDescription = "Add subtask",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Set Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogFieldLabel("Set Reminder")
                if (reminderText.isNotEmpty() && isEditing) {
                    Text(
                        text = "Clear",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { reminderText = "" }
                            .padding(bottom = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .then(if (isEditing) Modifier.clickable { showDatePicker = true } else Modifier)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminderText.ifEmpty { "MM/DD/YYYY" },
                    fontSize = 12.sp,
                    color = if (reminderText.isEmpty()) DialogLabel else Color.Black
                )
                Icon(
                    painter = painterResource(id = R.drawable.icon_calendar),
                    contentDescription = "Pick date",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Priority Selection
            DialogFieldLabel("Priority")
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .then(if (isEditing) Modifier.clickable { priorityMenuExpanded = true } else Modifier)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (priority) {
                            TaskPriority.TOP -> "Top priority"
                            TaskPriority.MEDIUM -> "Medium priority"
                            TaskPriority.LOW -> "Low priority"
                        },
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
                DropdownMenu(
                    expanded = priorityMenuExpanded,
                    onDismissRequest = { priorityMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Top priority") },
                        onClick = {
                            priority = TaskPriority.TOP
                            priorityMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Medium priority") },
                        onClick = {
                            priority = TaskPriority.MEDIUM
                            priorityMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Low priority") },
                        onClick = {
                            priority = TaskPriority.LOW
                            priorityMenuExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Notes field
            DialogFieldLabel("Notes")
            TaskDialogTextField(
                value = notes,
                onValueChange = { notes = it },
                singleLine = false,
                readOnly = !isEditing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Save / Add button
            if (isEditing) {
                val canSave = title.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DialogButton.copy(alpha = if (canSave) 1f else 0.4f))
                        .clickable(enabled = canSave) {
                            onSave(
                                title.trim(),
                                description.trim(),
                                taskReminderToMillis(reminderText),
                                priority,
                                notes.trim(),
                                localSubtaskTitles.toList()
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCreateMode) "Add Task" else "Save Changes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showDatePicker && isEditing) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = taskReminderToMillis(reminderText)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { reminderText = millisToTaskReminder(it) }
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

@Composable
private fun SubtaskRowItem(
    title: String,
    isDone: Boolean,
    onToggleDone: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskCheckbox(
            checked = isDone,
            onCheckedChange = onToggleDone,
            size = 18.dp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = if (isDone) DialogLabel else Color.Black,
            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            painter = painterResource(id = R.drawable.icon_delete),
            contentDescription = "Remove subtask",
            tint = DeleteIcon,
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun DialogFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = DialogLabel,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun TaskDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        readOnly = readOnly,
        textStyle = TextStyle(fontSize = 12.sp, color = Color.Black),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    AIOProdAppTheme {
        TasksScreen()
    }
}