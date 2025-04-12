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
import java.awt.datatransfer.DataFlavor

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColumnScope.AppContent(applicationScope: CoroutineScope) {
    val isLoggedIn by gC.isLoggedIn
    val googleSheetsId by gC.googleSheetsId
    val sheetsFileNameState by gC.sheetsFileName
    val isAppBusy by gC.isAppBusy
    val darkGray by gC.darkGray
    val middleGray by gC.middleGray
    val lightGray by gC.lightGray

    suspend fun ensureSheetSelectedOrPrompt() {
        if (!isLoggedIn) {
            if (GoogleSheetsHelper.login()) {gC.googleSheetsManager.refreshAvailableSheets(); gC.showSheetsModal.value = true; gC.consoleMessage.value = ConsoleMessage("️✅ Connecté au compte Google.", ConsoleMessageType.INFO)}
            else {gC.consoleMessage.value = ConsoleMessage("❌ Échec de la connexion Google.", ConsoleMessageType.ERROR)}
        }
        else {gC.googleSheetsManager.refreshAvailableSheets(); gC.showSheetsModal.value = true}
    }

    suspend fun onAddButtonClick() {
        if (googleSheetsId.isEmpty()) {gC.consoleMessage.value = ConsoleMessage("❌ Aucune feuille Google Sheets sélectionnée", ConsoleMessageType.ERROR); ensureSheetSelectedOrPrompt(); return}
        gC.isExtractionLoading.value = true; gC.isExportationLoading.value = true
        var clipboardContent: String? = null
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            // Boucles de détection de la page de profil
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {clipboardContent = clipboard.getData(DataFlavor.stringFlavor) as? String}
            if (clipboardContent.isNullOrBlank()) {gC.consoleMessage.value = ConsoleMessage("❌ Presse-papiers vide ou contenu non textuel.", ConsoleMessageType.ERROR); return}
            bot().changeApp()
            if (!bot().detect(clipboard) {}) {gC.consoleMessage.value = ConsoleMessage("❌ Page de profil non détectée ou erreur de détection.", ConsoleMessageType.ERROR); return}
            bot().changeApp()
            // Démarrage de l'analyse du texte
            gC.consoleMessage.value = ConsoleMessage("⏳ Analyse du profil en cours...", ConsoleMessageType.INFO)
            gC.profileParser.processInput(applicationScope, clipboardContent)
            // Ajout au fichier Google Sheets
            if (gC.currentProfile.value == null) {gC.consoleMessage.value = ConsoleMessage("❌ Aucune donnée de profil extraite.", ConsoleMessageType.ERROR); return}
            // Ajout au fichier Google Sheets
            gC.consoleMessage.value = ConsoleMessage("⏳ Synchronisation Google Sheets en cours...", ConsoleMessageType.INFO)
            gC.googleSheetsManager.addLineToGoogleSheets()

        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur pendant l'extraction/export: ${e.message}", ConsoleMessageType.ERROR)}
        finally {gC.isExtractionLoading.value = false; gC.isExportationLoading.value = false}
    }

    fun onGoogleButtonClick() {applicationScope.launch {ensureSheetSelectedOrPrompt()}}

    val currentSheetFileName = if (sheetsFileNameState.isNotEmpty()) sheetsFileNameState else "Aucun fichier chargé"
    Text("Source Google Sheets : $currentSheetFileName", color = lightGray)
    Spacer(Modifier.height(10.dp))
    // Barre de boutons
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(darkGray).padding(5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(Modifier.padding(start = 5.dp), Arrangement.Start, Alignment.CenterVertically) {
            // Bouton de choix de fichier
            Card({onGoogleButtonClick()}, Modifier, !isAppBusy, RoundedCornerShape(50), backgroundColor = if (!isAppBusy) Color(0xFF34A853) else darkGray, contentColor = Color.White, border = BorderStroke(1.dp, darkGray), elevation = if (!isAppBusy) 5.dp else 0.dp) {
                Icon(Icons.Filled.AttachFile, "Choisir/Changer Feuille Google Sheets", Modifier.size(50.dp).padding(10.dp), tint = Color.White)
            }
            Spacer(Modifier.width(5.dp))
            // Bouton d'ajout Google Sheets
            val canAddLine = !isAppBusy && googleSheetsId.isNotEmpty()
            Card({applicationScope.launch {onAddButtonClick()}}, Modifier, canAddLine, RoundedCornerShape(50), backgroundColor = if (canAddLine) middleGray else darkGray, contentColor = if (canAddLine) lightGray else Color.DarkGray, border = BorderStroke(1.dp, darkGray), elevation = if (canAddLine) 5.dp else 0.dp) {
                Icon(Icons.Filled.Add, "Ajouter les données du profil à Google Sheets", Modifier.size(50.dp).padding(10.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(50)).background(darkGray).padding(10.dp), Alignment.Center) {Icon(Icons.Filled.AddToDrive, "Google Drive", tint = lightGray)}
    }
}