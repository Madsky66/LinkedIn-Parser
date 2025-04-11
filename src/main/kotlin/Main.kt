import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import config.GlobalInstance.config as gC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import manager.AppDataManager
import ui.composable.AppTitleBar
import ui.composable.app.AppContent
import ui.composable.app.StatusBar
import ui.composable.modal.SettingsModal
import ui.composable.modal.SheetsPickerModal
import utils.*
import utils.GoogleSheetsHelper.loadAvailableSheets

fun main() = application {
    val applicationScope: CoroutineScope = rememberCoroutineScope()
    var windowState by remember {mutableStateOf(WindowState(size = DpSize(600.dp, 250.dp)))}
    val darkGray = gC.darkGray.value
    val middleGray = gC.middleGray.value

    LaunchedEffect(Unit) {AppDataManager.applyAppData()}
    LaunchedEffect(gC.isDarkTheme.value) {gC.updateThemeColors()}
    LaunchedEffect(gC.isExtractionLoading.value, gC.isImportationLoading.value, gC.isExportationLoading.value) {gC.isAppBusy.value = gC.isExtractionLoading.value || gC.isImportationLoading.value || gC.isExportationLoading.value}
    LaunchedEffect(gC.googleSheetsId.value) {
        if (gC.googleSheetsId.value.isNotEmpty()) {
            try {
                val name = GoogleSheetsHelper.getSheetName(gC.googleSheetsId.value)
                gC.sheetsFileName.value = name
            }
            catch (e: Exception) {gC.sheetsFileName.value = "Fichier inconnu"}
        }
        else {gC.sheetsFileName.value = ""}
    }

    // Modale des paramètres
    if (gC.showSettingsModal.value) {SettingsModal(applicationScope) {gC.showSettingsModal.value = false}}
    // Modale de sélection de feuille Google Sheets
    if (gC.showSheetsModal.value) {
        LaunchedEffect(Unit) {loadAvailableSheets()}
        SheetsPickerModal(
            spreadsheets = gC.availableSheets.value,
            onFileSelected = {id -> applicationScope.launch {gC.googleSheetsManager.selectGoogleSheetsFile(id); gC.showSheetsModal.value = false}},
            onCreateNew = {title ->
                applicationScope.launch {
                    try {
                        val newId = GoogleSheetsHelper.createNewSheet(title)
                        gC.googleSheetsManager.selectGoogleSheetsFile(newId)
                        gC.showSheetsModal.value = false
                    }
                    catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de la création de la feuille: ${e.message}", ConsoleMessageType.ERROR)}
                }
            },
            onDismiss = {gC.showSheetsModal.value = false}
        )
    }

    Window({exitApplication()}, windowState, visible = true, "LinkedIn Parser", undecorated = true) {
        Column(Modifier.fillMaxSize()) {
            // Barre de titre
            WindowDraggableArea(Modifier.fillMaxWidth().height(50.dp).background(darkGray)) {
                Row(Modifier.fillMaxSize()) {AppTitleBar(gC, {onMinimizeWindow(windowState)}, {onCloseApp()})}
            }
            // Menu
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().background(gC.middleGray.value).padding(20.dp), Arrangement.SpaceEvenly, Alignment.CenterHorizontally) {
                    AppContent(applicationScope)
                    Spacer(Modifier.height(5.dp))
                    StatusBar()
                }
            }
        }
    }
}