package ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.*
import config.GlobalConfig

@Composable
fun AppTitleBar(gC: GlobalConfig, onMinimizeWindow: () -> Unit, onCloseApp: () -> Unit) {
    val lightGray = gC.lightGray.value

    Row(Modifier.fillMaxSize().padding(15.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // Titre
        Row(Modifier.fillMaxHeight(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            IconButton({gC.showSettingsModal.value = !gC.showSettingsModal.value}, Modifier.size(25.dp).clip(RoundedCornerShape(100))) {Icon(Icons.Filled.Settings, "Paramètres", tint = lightGray)} // Bouton de menu
            Spacer(Modifier.width(15.dp)) // Spacer
            Text("LinkedIn Parser", fontSize = 15.sp, color = lightGray) // Texte
        }
        // Boutons
        Row(Modifier.fillMaxHeight(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            IconButton(onMinimizeWindow, Modifier.size(25.dp).clip(RoundedCornerShape(100))) {Icon(Icons.Filled.KeyboardArrowDown, "Minimiser", tint = lightGray)} // Minimiser
            Spacer(Modifier.width(15.dp)) // Spacer
            IconButton(onCloseApp, Modifier.size(25.dp).clip(RoundedCornerShape(100))) {Icon(Icons.Filled.Close, "Quitter", tint = lightGray)} // Quitter
        }
    }
}