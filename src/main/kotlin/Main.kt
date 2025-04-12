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
import utils.*

fun main() = application {
    val applicationScope: CoroutineScope = rememberCoroutineScope()
    val windowState = rememberWindowState(size = DpSize(600.dp, 250.dp))

    LaunchedEffect(Unit) {
        AppDataManager.applyAppData()
        val initialSheetId = gC.googleSheetsId.value
        if (initialSheetId.isNotEmpty()) {applicationScope.launch {gC.googleSheetsManager.selectGoogleSheetsFile(initialSheetId)}}
    }
    LaunchedEffect(gC.isDarkTheme.value) {gC.updateThemeColors()}

    // Modale des paramètres
    if (gC.showSettingsModal.value) {SettingsModal(applicationScope) {gC.showSettingsModal.value = false}}
    // Modale Google Sheets
    if (gC.showSheetsModal.value) {SheetsPickerModal(gC.availableSheets.value, {id -> applicationScope.launch {gC.googleSheetsManager.selectGoogleSheetsFile(id); gC.showSheetsModal.value = false}}, {title -> applicationScope.launch {gC.googleSheetsManager.createAndSelectSheet(title); gC.showSheetsModal.value = false}}, {gC.showSheetsModal.value = false})}

    Window({onCloseApp()}, windowState, visible = true, "LinkedIn Parser", undecorated = true, transparent = false) {
        val darkGray = gC.darkGray.value
        val middleGray = gC.middleGray.value

        Column(Modifier.fillMaxSize()) {
            WindowDraggableArea(Modifier.fillMaxWidth().height(50.dp).background(darkGray)) {Row(Modifier.fillMaxSize()) {AppTitleBar(gC, {onMinimizeWindow(windowState)}, {onCloseApp()})}}
            Column(Modifier.fillMaxSize().background(middleGray).padding(20.dp), Arrangement.SpaceBetween, Alignment.CenterHorizontally) {
                AppContent(applicationScope)
                Spacer(Modifier.height(5.dp))
                StatusBar()
            }
        }
    }
}