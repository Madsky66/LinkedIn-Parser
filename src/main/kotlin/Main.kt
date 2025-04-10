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

fun main() = application {
    val applicationScope: CoroutineScope = rememberCoroutineScope()
    var windowState by remember {mutableStateOf(WindowState(size = DpSize(600.dp, 250.dp)))}

    LaunchedEffect(Unit) {AppDataManager.applyAppData()}
    LaunchedEffect(gC.isDarkTheme.value) {gC.updateThemeColors()}

    val darkGray = gC.darkGray.value
    val middleGray = gC.middleGray.value

    var sheetId by remember {mutableStateOf("")}
    var availableSheets by remember {mutableStateOf<List<Pair<String, String>>>(emptyList())}
    var isLoadingSheets by remember {mutableStateOf(false)}

    // Modale des paramètres
    if (gC.showSettingsModal.value) {SettingsModal(applicationScope) {gC.showSettingsModal.value = false}}
    // Modale de sélection de feuille Google Sheets
    if (gC.showSheetsModal.value) {
        SheetsPickerModal(
            spreadsheets = availableSheets,
            onFileSelected = {id -> sheetId = id; gC.showSheetsModal.value = false},
            onCreateNew = {title ->
                applicationScope.launch {
                    try {
                        val newId = GoogleSheetsHelper.createNewSpreadsheet(title)
                        sheetId = newId
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