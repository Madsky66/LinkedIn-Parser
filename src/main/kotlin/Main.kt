import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import config.GlobalInstance.config as gC
import kotlinx.coroutines.*
import manager.AppDataManager
import ui.composable.AppTitleBar
import ui.composable.app.*
import ui.composable.modal.*

fun main() = application {
    val applicationScope: CoroutineScope = rememberCoroutineScope()
    val windowState = rememberWindowState(size = DpSize(600.dp, 250.dp))
    val onCloseApp = {gC.appDataManager.saveAppData(); exitApplication()}
    val onMinimizeWindow = {windowState.isMinimized = true}

    LaunchedEffect(Unit) {
        AppDataManager.applyAppData()
        val initialSheetId = gC.googleSheetsId.value
        if (initialSheetId.isNotEmpty()) {applicationScope.launch {gC.googleSheetsManager.selectGoogleSheetsFile(initialSheetId)}}
    }
    LaunchedEffect(gC.isDarkTheme.value) {gC.updateThemeColors()}

    // Modale des paramètres
    if (gC.showSettingsModal.value) {SettingsModal(applicationScope) {gC.showSettingsModal.value = false}}
    // Modale Google Sheets
    if (gC.showSheetsModal.value) {
        val hideSheetModal = {gC.showSheetsModal.value = false}
        SheetsPickerModal(
            gC.availableSheets.value,
            {id -> applicationScope.launch {gC.googleSheetsManager.selectGoogleSheetsFile(id); hideSheetModal()}},
            {title -> applicationScope.launch {gC.googleSheetsManager.createAndSelectSheet(title); hideSheetModal()}},
            hideSheetModal
        )
    }

    Window(onCloseApp, windowState, visible = true, "LinkedIn Parser", undecorated = true, transparent = false) {
        val darkGray = gC.darkGray.value
        val middleGray = gC.middleGray.value

        Column(Modifier.fillMaxSize()) {
            WindowDraggableArea(Modifier.fillMaxWidth().height(50.dp).background(darkGray)) {Row(Modifier.fillMaxSize()) {AppTitleBar(gC, onMinimizeWindow, onCloseApp)}}
            Column(Modifier.fillMaxSize().background(middleGray).padding(20.dp), Arrangement.SpaceBetween, Alignment.CenterHorizontally) {
                AppContent(applicationScope)
                Spacer(Modifier.height(5.dp))
                StatusBar()
            }
        }
    }
}