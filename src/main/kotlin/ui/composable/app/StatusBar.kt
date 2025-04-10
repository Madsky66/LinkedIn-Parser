package ui.composable.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import config.GlobalInstance.config as gC
import utils.ConsoleMessageType

@Composable
fun ColumnScope.StatusBar() {
    val statusColor = when (gC.consoleMessage.value.type) {
        ConsoleMessageType.SUCCESS -> Color.Green.copy(0.9f)
        ConsoleMessageType.ERROR -> Color.Red.copy(0.9f)
        ConsoleMessageType.WARNING -> Color.Yellow.copy(0.9f)
        else -> gC.lightGray.value
    }
    // Console de statut
    Column(Modifier.fillMaxWidth().background(Color.Black), Arrangement.Center) {
        Text(gC.consoleMessage.value.message, Modifier.padding(20.dp, 10.dp), fontSize = 15.sp, color = statusColor)
    }
}