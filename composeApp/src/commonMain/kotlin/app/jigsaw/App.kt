package app.jigsaw

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF173C35)
private val Paper = Color(0xFFF7F5EF)

@Composable
fun App() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, background = Paper, surface = Color.White)) {
        Column(Modifier.fillMaxSize().background(Paper).padding(28.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("jigsaw", fontSize = 32.sp, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("A little space to piece things together", color = Ink)
            }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Card(Modifier.width(240.dp).fillMaxHeight()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Your next quiet moment", style = MaterialTheme.typography.headlineSmall)
                        Text("Choose a photograph, settle in, and make something whole.")
                        Button(onClick = {}) { Text("New puzzle") }
                        OutlinedButton(onClick = {}) { Text("Use your own image") }
                        HorizontalDivider()
                        Text("PIECE STASH", style = MaterialTheme.typography.labelMedium)
                        Text("Organize by color, shape, or your own intuition.")
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE7ECE6)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Every piece belongs.", style = MaterialTheme.typography.headlineLarge, color = Ink)
                        Spacer(Modifier.height(12.dp))
                        Text("Your puzzle table is ready.", color = Ink)
                    }
                }
            }
        }
    }
}
