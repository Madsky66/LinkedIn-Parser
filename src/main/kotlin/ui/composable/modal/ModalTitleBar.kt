package ui.composable.modal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import config.GlobalConfig

@Composable
fun ModalTitleBar(gC: GlobalConfig, title: String, onCloseApp: () -> Unit) {
    val lightGray = gC.lightGray.value

    Row(Modifier.fillMaxSize().padding(15.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // Titre
        Row(Modifier.fillMaxHeight(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(title, fontSize = 15.sp, color = lightGray) // Texte
        }
        // Boutons
        Row(Modifier.fillMaxHeight(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            IconButton(onCloseApp, Modifier.size(25.dp).clip(RoundedCornerShape(100))) {Icon(Icons.Filled.Close, "Quitter", tint = lightGray)} // Quitter
        }
    }
}