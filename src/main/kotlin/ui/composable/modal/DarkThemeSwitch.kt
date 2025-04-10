package ui.composable.modal


import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import manager.AppDataManager
import config.GlobalInstance.config as gC

@Composable
fun DarkThemeSwitch() {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("Activer / désactiver le thème sombre", color = gC.lightGray.value, style = MaterialTheme.typography.body2)
        Switch(
            checked = gC.isDarkTheme.value,
            onCheckedChange = {newValue -> gC.isDarkTheme.value = newValue; AppDataManager.updateTheme(newValue)},
            modifier = Modifier.semantics {contentDescription = "Basculer entre le thème clair et sombre"},
            colors = SwitchDefaults.colors(
                checkedThumbColor = gC.lightGray.value,
                uncheckedThumbColor = gC.darkGray.value,
                checkedTrackColor = gC.lightGray.value.copy(0.5f),
                uncheckedTrackColor = gC.darkGray.value.copy(0.5f)
            )
        )
    }
}