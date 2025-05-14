package utils

import kotlinx.coroutines.delay
import java.awt.Robot
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import config.GlobalInstance.config as gC

class BotController {
    val robot = Robot()
    suspend fun detect(clipboard: Clipboard, onNewClipboardContent: (String) -> Unit): Boolean {
        var attempts = 1
        var clipboardContent = copyPageContent(robot, clipboard)
        var isProfileDetected = clipboardContent.lines().take(5).any {it.contains("dialogue")} && clipboardContent.length > 5000
        while (!isProfileDetected && attempts < 100) {
            attempts++
            gC.consoleMessage.value = ConsoleMessage("⏳ Détection de la page en cours... [Tentative : $attempts]", ConsoleMessageType.INFO)
            openCoordsDialog(robot, clipboard)
            delay(100)
            clipboardContent = copyPageContent(robot, clipboard)
            isProfileDetected = clipboardContent.lines().take(5).any {it.contains("dialogue")} && clipboardContent.length > 5000
        }
        if (!isProfileDetected) {gC.consoleMessage.value = ConsoleMessage("❌ La page de profil n'a pas été détectée après $attempts tentatives", ConsoleMessageType.ERROR); return false}
        onNewClipboardContent(clipboardContent)
        return true
    }

    fun Robot.ctrlAnd(key: Int) {
        keyPress(KeyEvent.VK_CONTROL)    // <--------------------------------------------- Touche "Ctrl" pressée
        delay(10)
        keyPress(key)    // <------------------------------------------------------------- Touche secondaire pressée
        delay(10)
        keyRelease(key)    // <----------------------------------------------------------- Touche secondaire relâchée
        delay(10)
        keyRelease(KeyEvent.VK_CONTROL)    // <------------------------------------------- Touche "Ctrl" relâchée
    }

    suspend fun copyPageContent(robot: Robot, clipboard: Clipboard): String {
        robot.ctrlAnd(KeyEvent.VK_A)    // <---------------------------------------------- "Ctrl" + "A"
        delay(50)
        robot.ctrlAnd(KeyEvent.VK_C)    // <---------------------------------------------- "Ctrl" + "C"
        delay(50)
        val clipboardContent = try {clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""} catch (e: Exception) {println("Erreur : $e")}
        return clipboardContent.toString()
    }

    suspend fun openCoordsDialog(robot: Robot, clipboard: Clipboard): String {
        robot.ctrlAnd(KeyEvent.VK_L)    // <---------------------------------------------- "Ctrl" + "L"
        delay(50)
        robot.ctrlAnd(KeyEvent.VK_C)    // <---------------------------------------------- "Ctrl" + "C"
        delay(50)
        var clipboardContent = try {clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""} catch (e: Exception) {println("Erreur : $e")}
        if (clipboardContent != "") clipboardContent = "${clipboardContent}overlay/contact-info/"
        robot.ctrlAnd(KeyEvent.VK_V)    // <---------------------------------------------- "Ctrl" + "V"
        delay(50)
        robot.keyPress(KeyEvent.VK_ENTER)    // <----------------------------------------- "Enter"
        delay(50)
        return clipboardContent.toString()
    }

    fun changeApp() {
        robot.keyPress(KeyEvent.VK_ALT)    // <------------------------------------------------- Touche "Alt" pressée
        robot.delay(10)
        robot.keyPress(KeyEvent.VK_TAB)    // <------------------------------------------------- Touche secondaire pressée
        robot.delay(10)
        robot.keyRelease(KeyEvent.VK_TAB)    // <----------------------------------------------- Touche secondaire relâchée
        robot.delay(10)
        robot.keyRelease(KeyEvent.VK_ALT)    // <----------------------------------------------- Touche "Alt" relâchée
        robot.delay(10)
    }
}