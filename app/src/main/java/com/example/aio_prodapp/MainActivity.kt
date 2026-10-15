package com.example.aio_prodapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme

/*
Kulang kapa ng onClick functionality,
Recyclesitems (ung cards na reusable for the notes)
ung status summary like, 3 notes 2reminders. and the rest of the screens to do.
*/

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            AIOProdAppTheme {
                MainAppStructure()
            }
        }
    }
}

@Composable
fun MainAppStructure(startIndex: Int = 0) {
    // The selected tab lives here so both the bottom bar and the content can use it
    var selectedIndex by remember { mutableIntStateOf(startIndex) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar() },
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
                0 -> MyNotesScreen()   // lives in NotesScreen.kt
                2 -> TasksScreen()     // lives in TasksScreen.kt
                else -> { /* Focus and Profile screens coming later */ }
            }
        }
    }
}

@Composable
fun TopBar() {
    Surface(color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_hamburg),
                    contentDescription = null
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_bell),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun BottomBar(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
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
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
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