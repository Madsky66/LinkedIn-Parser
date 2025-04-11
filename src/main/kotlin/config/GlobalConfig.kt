package config

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import data.ProspectData
import manager.*
import utils.*

data class GlobalConfig(
    var isDarkTheme: MutableState<Boolean> = mutableStateOf(true),
    val themeColors: Colors = Colors(),
    var darkGray: MutableState<Color> = mutableStateOf<Color>(Color(0xFF2A2A2A)),
    var middleGray: MutableState<Color> = mutableStateOf<Color>(Color.DarkGray),
    var lightGray: MutableState<Color> = mutableStateOf<Color>(Color.LightGray),

    val googleSheetsManager: GoogleSheetsManager = GoogleSheetsManager(),
    val apolloManager: ApolloManager = ApolloManager(),
    val profileParser: ProfileParser = ProfileParser(),

    var showSettingsModal: MutableState<Boolean> = mutableStateOf(false),
    var showConfirmModal: MutableState<Boolean> = mutableStateOf(false),
    var showSheetsModal: MutableState<Boolean> = mutableStateOf(false),

    var isExtractionLoading: MutableState<Boolean> = mutableStateOf(false),
    var isImportationLoading: MutableState<Boolean> = mutableStateOf(false),
    var isExportationLoading: MutableState<Boolean> = mutableStateOf(false),
    var isAppBusy: MutableState<Boolean> = mutableStateOf(isExtractionLoading.value || isImportationLoading.value || isExportationLoading.value),

    var consoleMessage: MutableState<ConsoleMessage> = mutableStateOf(ConsoleMessage("En attente de données...", ConsoleMessageType.INFO)),
    var currentProfile: MutableState<ProspectData?> = mutableStateOf(null),

    var isLoggedIn: MutableState<Boolean> = mutableStateOf(false),
    var availableSheets: MutableState<List<Pair<String, String>>> = mutableStateOf(emptyList()),
    var googleSheetsId: MutableState<String> = mutableStateOf(""),
    var sheetsFileName: MutableState<String> = mutableStateOf(""),

    var apiKey: MutableState<String> = mutableStateOf(""),
    var pastedApiKey: MutableState<String> = mutableStateOf(""),
) {
    fun updateThemeColors() {
        val colors = themeColors.get(isDarkTheme)
        darkGray.value = colors[0]
        middleGray.value = colors[1]
        lightGray.value = colors[2]
    }
}

object GlobalInstance {
    val config = GlobalConfig()
    init {config.updateThemeColors()}
}