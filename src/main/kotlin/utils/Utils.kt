package utils

import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowState
import manager.AppDataManager
import kotlin.system.exitProcess

data class ConsoleMessage(val message: String, val type: ConsoleMessageType)
enum class ConsoleMessageType {INFO, SUCCESS, ERROR, WARNING}

fun onMinimizeWindow(windowState: WindowState) {windowState.isMinimized = true}
fun onCloseApp() {AppDataManager.saveAppData(); exitProcess(0)}

class Colors {
    private val darkThemeColors = listOf(
        Color(0xFF2A2A2A),  // ---------------> darkGray
        Color.DarkGray,     // ---------------> middleGray
        Color.LightGray     // ---------------> lightGray
    )
    private val lightThemeColors = listOf(
        Color(0xFF0A66C2),  // ---------------> darkBlue
        Color(0xFF03A9F4),  // ---------------> lightBlue
        Color.White         // ---------------> white
    )
    fun get(isDarkTheme: MutableState<Boolean>): List<Color> {return if (isDarkTheme.value) darkThemeColors else lightThemeColors}
}

@Composable
fun getButtonColors(backgroundColor: Color, disabledBackgroundColor: Color, contentColor: Color) = ButtonDefaults.buttonColors(backgroundColor, contentColor, disabledBackgroundColor, contentColor.copy(0.5f))