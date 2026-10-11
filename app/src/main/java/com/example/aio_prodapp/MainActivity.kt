package com.example.aio_prodapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.aio_prodapp.ui.theme.AIOProdAppTheme
import org.intellij.lang.annotations.JdkConstants
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
fun MainAppStructure() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar() },
        bottomBar = { BottomBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // My screen content
            MyNotesScreen()
        }
    }
}

@Composable
fun MyNotesScreen() {
    Column() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical =  8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "my notes",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontSize = 50.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(5.dp),
            )
            Row (
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,

            ){
                Icon(
                    painterResource(id = R.drawable.icon_calendar),
                    contentDescription = "Calendar",
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color = MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)



                )
                AddNote()
            }
        }

    }

}

@Composable
fun AddNote(){
    Row(
        Modifier
            .padding(1.dp)
            .clip(shape = RoundedCornerShape(4.dp))
            .background(color = MaterialTheme.colorScheme.secondary)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.icon_add),
            contentDescription = "Add note",
            tint = Color.White,
            modifier = Modifier.padding(5.dp)

        )
        Text(
            text = "Add note",
            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
            fontSize = 25.sp,
            color = Color.White,
            modifier = Modifier.padding(
                start = 1.dp,   // Keeps your left space
                top = 4.dp,      // Keeps your top space
                end = 14.dp,     // Keeps your right space
                bottom = 0.dp   // Changes the space below the text
            )
        )

    }
}

@Composable
fun TopBar() {
    Surface(color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, vertical =  8.dp),
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
fun BottomBar() {
    var selectedIndex by remember { mutableIntStateOf(0) }

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
                onClick = { selectedIndex = index }
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