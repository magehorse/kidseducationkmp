package com.kidsplay.kidseducation.screens

import com.kidsplay.kidseducation.platform.LocalDeps
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.nav.Navigator
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kidsplay.kidseducation.AppConfig
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard

@Composable
fun SettingsScreen(nav: Navigator, onBack: () -> Unit) {
    val app = LocalAppState.current
    var name by remember { mutableStateOf(app.userName) }

    ScreenFrame("Settings", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Parent corner: the title parents look for, then the "Parent" button opens the daily history.
            TileCard(Modifier.fillMaxWidth(), borderColor = KidsColors.Sun, onClick = { nav.push(Screen.ChildActivity) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("👨‍👩‍👧", fontSize = 34.sp)
                    Column(Modifier.weight(1f)) {
                        Text("Child Activity", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                        Text("See what your child learned and played every day", fontSize = 12.sp, color = Color.Gray)
                    }
                    Text(
                        "Parent ›", color = Color.White, fontSize = 13.sp, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(KidsColors.HeaderBottom).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            TileCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your name", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(20); app.updateName(name) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            TileCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sound", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = app.soundEnabled, onCheckedChange = { app.updateSound(it) })
                }
            }
            val deps = LocalDeps.current
            val openMyDrawings = deps.openMyDrawings
            if (openMyDrawings != null) {
                // Pictures the child saved in the drawing editor.
                TileCard(Modifier.fillMaxWidth(), borderColor = KidsColors.Sun, onClick = openMyDrawings) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🎨", fontSize = 34.sp)
                        Column(Modifier.weight(1f)) {
                            Text("My Drawings", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                            Text("See the pictures you saved", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(
                            "Open ›", color = Color.White, fontSize = 13.sp, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(KidsColors.HeaderBottom).padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            BigButton("Privacy Policy", { app.openUrl(AppConfig.PRIVACY_POLICY_URL) }, Modifier.fillMaxWidth())
            BigButton("Rate this app", { app.openUrl(AppConfig.STORE_URL) }, Modifier.fillMaxWidth(), color = KidsColors.Sun)
            Text("${AppConfig.APP_NAME} · version ${AppConfig.VERSION_NAME}", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
