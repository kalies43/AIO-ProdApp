package com.example.aio_prodapp

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme

// ---------- Data ----------

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed")
}

data class Task(
    val id: Int,
    val title: String,
    val date: String,
    val status: TaskStatus
)

// ---------- Look & feel helpers ----------

// Opaque light mint (mint blended onto white). Opaque on purpose:
// a see-through background lets the drop shadow show through and turns it gray.
@Composable
private fun mint(strength: Float = 0.4f): Color =
    MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = strength)
        .compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)

// Delete button colors from the design (tuned for light mode)
private val DeleteContainer = Color(0xFFF8C4C4)
private val DeleteIcon = Color(0xFFB03A3A)

private fun softShadow(alpha: Float = 0.2f) = Shadow(
    radius = 6.dp,
    spread = 0.dp,
    color = Color.Black,
    offset = DpOffset(x = 0.dp, y = 4.dp),
    alpha = alpha
)

// ---------- Screen ----------

@Composable
fun TasksScreen() {
    val tasks = remember {
        mutableStateListOf(
            Task(1, "Finish Assignment", "August 30, 2026", TaskStatus.IN_PROGRESS),
            Task(2, "Workout- Legs", "September 6, 2026", TaskStatus.TODO),
            Task(3, "Laundry", "September 8, 2026", TaskStatus.TODO),
            Task(4, "Buy groceries", "September 10, 2026", TaskStatus.COMPLETED)
        )
    }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<TaskStatus?>(null) } // null = All
    var selectedTaskId by remember { mutableStateOf<Int?>(1) }

    val visibleTasks = tasks.filter {
        (filter == null || it.status == filter) &&
                it.title.contains(query, ignoreCase = true)
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
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.weight(1f)
            )
            AddTaskButton(onClick = { /* TODO: open add-task dialog */ })
        }

        Spacer(Modifier.height(18.dp))

        // Filter chips
        val filters = listOf<TaskStatus?>(null) + TaskStatus.entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { status ->
                FilterChipItem(
                    label = status?.label ?: "All",
                    selected = filter == status,
                    onClick = { filter = status }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // The "RecyclerView": only visible items are composed and recycled
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            items(visibleTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    selected = task.id == selectedTaskId,
                    onClick = { selectedTaskId = task.id },
                    onEdit = { /* TODO: edit task */ },
                    onDelete = { tasks.removeAll { it.id == task.id } }
                )
            }
        }
    }
}

// ---------- Reusable pieces ----------

@Composable
fun TaskItem(
    task: Task,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val contentAlpha = if (selected) 1f else 0.6f

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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = task.date,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
        }

        Row(
            modifier = Modifier.alpha(contentAlpha),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TaskActionButton(
                iconRes = R.drawable.icon_edit, // swap for a pencil drawable if you add one
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

@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    AIOProdAppTheme {
        MainAppStructure(startIndex = 2)
    }
}