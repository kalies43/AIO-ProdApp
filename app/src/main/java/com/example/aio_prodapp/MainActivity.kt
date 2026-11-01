package com.example.aio_prodapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aio_prodapp.debug.DebugPanelDialog
import com.example.aio_prodapp.debug.DebugTools
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme
import com.example.aio_prodapp.viewmodel.AppViewModelProvider
import com.example.aio_prodapp.viewmodel.NotificationsViewModel

class MainActivity : ComponentActivity() {

    private var initialNoteId = mutableStateOf<Int?>(null)
    private var initialTaskId = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        handleNotificationIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            AIOProdAppTheme {
                MainAppStructure(
                    initialNoteId = initialNoteId.value,
                    initialTaskId = initialTaskId.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra("note_id")) {
            val id = intent.getIntExtra("note_id", -1)
            if (id != -1) initialNoteId.value = id
        }
        if (intent.hasExtra("task_id")) {
            val id = intent.getIntExtra("task_id", -1)
            if (id != -1) initialTaskId.value = id
        }
    }
}

@Composable
fun MainAppStructure(
    initialNoteId: Int? = null,
    initialTaskId: Int? = null,
    notificationsViewModel: NotificationsViewModel? = if (LocalInspectionMode.current) null else viewModel(factory = AppViewModelProvider.Factory)
) {
    val isPreview = LocalInspectionMode.current
    val hasUnread = if (isPreview || notificationsViewModel == null) {
        true
    } else {
        notificationsViewModel.hasUnread.collectAsStateWithLifecycle().value
    }

    var pendingNoteId by remember(initialNoteId) { mutableStateOf(initialNoteId) }
    var pendingTaskId by remember(initialTaskId) { mutableStateOf(initialTaskId) }
    var showNotificationOverlay by remember { mutableStateOf(false) }

    var selectedIndex by remember(initialNoteId, initialTaskId) {
        mutableIntStateOf(
            when {
                initialTaskId != null -> 2
                initialNoteId != null -> 0
                else -> 0
            }
        )
    }

    var showDebugPanel by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                hasUnread = hasUnread,
                onBellClick = { showNotificationOverlay = true },
                onHamburgerLongPress = { showDebugPanel = true }
            )
        },
        bottomBar = {
            BottomBar(
                selectedIndex = selectedIndex,
                onTabSelected = { selectedIndex = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedIndex) {
                0 -> MyNotesScreen(
                    pendingNoteId = pendingNoteId,
                    onClearPendingNoteId = { pendingNoteId = null }
                )
                1 -> FocusScreen()
                2 -> TasksScreen(
                    pendingTaskId = pendingTaskId,
                    onClearPendingTaskId = { pendingTaskId = null }
                )
                3 -> ProfileScreen(onOpenDebugPanel = { showDebugPanel = true })
            }
        }
    }

    if (showNotificationOverlay) {
        NotificationHistoryOverlay(
            onDismiss = { showNotificationOverlay = false },
            onNavigateToNote = { noteId ->
                pendingNoteId = noteId
                selectedIndex = 0
                showNotificationOverlay = false
            },
            onNavigateToTask = { taskId ->
                pendingTaskId = taskId
                selectedIndex = 2
                showNotificationOverlay = false
            },
            onNavigateToFocus = {
                selectedIndex = 1
                showNotificationOverlay = false
            },
            viewModel = notificationsViewModel
        )
    }

    if (showDebugPanel) {
        DebugPanelDialog(onDismiss = { showDebugPanel = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopBar(
    hasUnread: Boolean = false,
    onBellClick: () -> Unit = {},
    onHamburgerLongPress: () -> Unit = {}
) {
    Surface(color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (DebugTools.enabled) {
                            Modifier.combinedClickable(onClick = { }, onLongClick = onHamburgerLongPress)
                        } else {
                            Modifier
                        }
                    )
            )
            IconButton(onClick = onBellClick) {
                Box {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_bell),
                        contentDescription = "Notifications"
                    )
                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        R.drawable.icon_note_edit to "Notes",
        R.drawable.icon_target to "Focus",
        R.drawable.icon_task_complete to "Tasks",
        R.drawable.icon_profile to "Profile"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, (iconRes, label) ->
            BottomBarItem(
                iconRes = iconRes,
                label = label,
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
fun BottomBarItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    val contentColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .then(
                if (selected) Modifier
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
                    .background(MaterialTheme.colorScheme.primaryContainer, shape)
                else Modifier
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = contentColor
        )
        Text(text = label, color = contentColor, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    AIOProdAppTheme {
        MainAppStructure()
    }
}