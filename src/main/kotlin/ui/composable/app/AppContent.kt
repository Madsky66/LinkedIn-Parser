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
import androidx.compose.ui.graphics.Color
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

    suspend fun onAddButtonClic() {
        if (gC.googleSheetsId.value.isNotEmpty()) {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            var clipboardContent = ""
            // Boucles de détection de la page de profil
            gC.isExtractionLoading.value = true
            bot().changeApp()
            if (!bot().detect(clipboard) {clipboardContent = it.toString()}) {gC.isExtractionLoading.value = false; return}
            else {gC.consoleMessage.value = ConsoleMessage("✅ Page de profil détectée et correctement chargée", ConsoleMessageType.SUCCESS)}
            bot().changeApp()
            // Démarrage de l'analyse du texte
            gC.consoleMessage.value = ConsoleMessage("⏳ Analyse des données en cours...", ConsoleMessageType.INFO)
            gC.profileParser.processInput(applicationScope, clipboardContent)
            // Ajout au fichier Google Sheets
            gC.isExportationLoading.value = true
            gC.consoleMessage.value = ConsoleMessage("⏳ Synchronisation en cours...", ConsoleMessageType.INFO)
            gC.googleSheetsManager.addLineToGoogleSheets()
            gC.isExportationLoading.value = false
        }
        else {
            if (!gC.isLoggedIn.value) {
                try {applicationScope.launch {if (GoogleSheetsHelper.login()) {gC.showSheetsModal.value = true}}}
                catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Connexion au compte Google impossible [$e]", ConsoleMessageType.ERROR)}
            }
            else {
                gC.consoleMessage.value = ConsoleMessage("❌ Aucune feuille Google Sheets sélectionnée", ConsoleMessageType.ERROR)
                return
            }
        }
    }

    fun onGoogleButtonClic() {
        if (gC.googleSheetsId.value.isNotEmpty()) {
            gC.consoleMessage.value = ConsoleMessage("⏳ En attente de sélection d'un fichier", ConsoleMessageType.INFO)
            gC.showSheetsModal.value = true
        }
        else {
            if (!gC.isLoggedIn.value) {
                try {applicationScope.launch {if (GoogleSheetsHelper.login()) {gC.showSheetsModal.value = true}}}
                catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Connexion au compte Google impossible [$e]", ConsoleMessageType.ERROR)}
            }
            else {
                gC.consoleMessage.value = ConsoleMessage("❌ Aucune feuille Google Sheets sélectionnée", ConsoleMessageType.ERROR)
                return
            }
        }
    }

    var sheetsFileName = if (gC.sheetsFileName.value != "") {gC.sheetsFileName.value} else {"Aucun fichier chargé"}

    Row(Modifier) {Text("Source Google Sheets : $sheetsFileName", color = lightGray)}
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(100)).background(darkGray).padding(5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(Modifier.padding(5.dp), Arrangement.End, Alignment.CenterVertically) {
            // Bouton de changement de fichier
            Card({onGoogleButtonClic()}, Modifier, !gC.isAppBusy.value, RoundedCornerShape(100), backgroundColor = if (!gC.isAppBusy.value) {Color(0xFF34A853)} else {darkGray}, contentColor = Color.White, border = BorderStroke(1.dp, darkGray), elevation = 10.dp) {
                Icon(Icons.Filled.AttachFile, "Google Sheets", Modifier.size(50.dp).padding(10.dp), tint = Color.White)
            }
            Spacer(Modifier.width(5.dp))
            // Bouton d'ajout Google Sheets
            Card({applicationScope.launch {onAddButtonClic()}}, Modifier, !gC.isAppBusy.value, RoundedCornerShape(100), backgroundColor = if (!gC.isAppBusy.value) {middleGray} else {darkGray}, contentColor = lightGray, border = BorderStroke(1.dp, darkGray), elevation = 10.dp) {
                Icon(Icons.Filled.Add, "", Modifier.size(50.dp).padding(10.dp), tint = lightGray)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(100)).background(gC.darkGray.value).padding(10.dp), Alignment.Center) {Icon(Icons.Filled.AddToDrive, "", tint = gC.lightGray.value)}
    }
}