package ui.composable.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import config.GlobalInstance.config as gC
import utils.BotController as bot
import utils.*
import java.awt.Toolkit

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColumnScope.AppContent(applicationScope: CoroutineScope) {
    val darkGray = gC.darkGray.value
    val middleGray = gC.middleGray.value
    val lightGray = gC.lightGray.value

    suspend fun onAddProfile() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        var clipboardContent = ""
        // Boucles de détection de la page de profil
        bot().changeApp()
        if (!bot().detect(clipboard) {clipboardContent = it.toString()}) {println("test"); return}
        else {gC.consoleMessage.value = ConsoleMessage("✅ Page de profil détectée et correctement chargée", ConsoleMessageType.SUCCESS)}
        bot().changeApp()
        // Démarrage de l'analyse du texte
        gC.consoleMessage.value = ConsoleMessage("⏳ Analyse des données en cours...", ConsoleMessageType.INFO)
        gC.linkedinManager.processInput(applicationScope, clipboardContent)
        // Ajout au fichier Google Sheets
        gC.consoleMessage.value = ConsoleMessage("⏳ Synchronisation en cours...", ConsoleMessageType.INFO)
        gC.googleSheetsManager.exportToGoogleSheets()
    }

    fun onSwapFile() {
        gC.consoleMessage.value = ConsoleMessage("⏳ En attente de sélection d'un fichier", ConsoleMessageType.INFO)
        gC.showSheetsModal.value = true
    }

    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(100)).background(darkGray).padding(5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(Modifier.padding(5.dp), Arrangement.End, Alignment.CenterVertically) {
            // Bouton de changement de fichier
            Card({onSwapFile()}, Modifier, !gC.isAppBusy.value, RoundedCornerShape(100), backgroundColor = if (!gC.isAppBusy.value) {middleGray} else {darkGray}, contentColor = lightGray, border = BorderStroke(1.dp, darkGray), elevation = 10.dp) {
                Icon(Icons.Filled.AttachFile, "", Modifier.size(50.dp).padding(10.dp), tint = lightGray)
            }
            Spacer(Modifier.width(5.dp))
            // Bouton d'ajout Google Sheets
            Card({applicationScope.launch {onAddProfile()}}, Modifier, !gC.isAppBusy.value, RoundedCornerShape(100), backgroundColor = if (!gC.isAppBusy.value) {middleGray} else {darkGray}, contentColor = lightGray, border = BorderStroke(1.dp, darkGray), elevation = 10.dp) {
                Icon(Icons.Filled.Add, "", Modifier.size(50.dp).padding(10.dp), tint = lightGray)
            }
            Spacer(Modifier.width(10.dp))
            Row(Modifier) {Text("Fichier GoogleSheets chargé : "/*$sheetsFileName"*/, color = lightGray)}
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(100)).background(gC.darkGray.value).padding(10.dp), Alignment.Center) {Icon(Icons.Filled.AddToDrive, "", tint = gC.lightGray.value)}
    }
}