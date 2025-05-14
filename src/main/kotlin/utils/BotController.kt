package utils

import kotlinx.coroutines.delay
import java.awt.Robot
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import config.GlobalInstance.config as gC

class BotController {
    private val robot = Robot()
    suspend fun detect(clipboard: Clipboard, onNewClipboardContent: (String) -> Unit): Boolean {
        var attempts = 1
        var clipboardContent = copyPageContent(clipboard)
        var isProfileDetected = clipboardContent.lines().take(5).any {it.contains("dialogue")} && clipboardContent.length > 5000
        while (!isProfileDetected && attempts < 100) {
            attempts++
            gC.consoleMessage.value = ConsoleMessage("⏳ Détection de la page en cours... [Tentative : $attempts]", ConsoleMessageType.INFO)
            openCoordsDialog(clipboard)
            delay(1000)
            clipboardContent = copyPageContent(clipboard)
            isProfileDetected = clipboardContent.lines().take(5).any {it.contains("dialogue")} && clipboardContent.length > 5000
            if (!isProfileDetected) {
                delay(1000)
                clipboardContent = copyPageContent(clipboard)
                isProfileDetected = clipboardContent.lines().take(5).any {it.contains("dialogue")} && clipboardContent.length > 5000
                if (!isProfileDetected) {
                    delay(3000)
                    clipboardContent = copyPageContent(clipboard)
                    isProfileDetected = clipboardContent.lines().take(5).any {it.contains("dialogue")} && clipboardContent.length > 5000
                    if (!isProfileDetected) {
                        delay(5000)
                        clipboardContent = copyPageContent(clipboard)
                        isProfileDetected = clipboardContent.lines().take(5).any {it.contains("dialogue")} && clipboardContent.length > 5000
                    }
                    else {break}
                }
                else {break}
            }
            else {break}
        }
        if (!isProfileDetected) {gC.consoleMessage.value = ConsoleMessage("❌ La page de profil n'a pas été détectée après $attempts tentatives", ConsoleMessageType.ERROR); return false}
        onNewClipboardContent(clipboardContent)
        return true
    }

    private suspend fun ctrlAnd(key: Int) {
        robot.keyPress(KeyEvent.VK_CONTROL)    // <---------------------------------------------- "Ctrl" + "C"
        robot.keyPress(key)    // <------------------------------------------------------------- Touche secondaire pressée
        delay(10)
        robot.keyRelease(key)    // <----------------------------------------------------------- Touche secondaire relâchée
        robot.keyRelease(KeyEvent.VK_CONTROL)    // <------------------------------------------- Touche "Ctrl" relâchée
        delay(10)
    }

    private suspend fun copyPageContent(clipboard: Clipboard): String {
        ctrlAnd(KeyEvent.VK_A)    // <---------------------------------------------- "Ctrl" + "A"
        delay(50)
        ctrlAnd(KeyEvent.VK_C)    // <---------------------------------------------- "Ctrl" + "C"
        delay(50)
        return try {clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""} catch (e: Exception) {println("Erreur : $e"); ""}
    }

    private suspend fun openCoordsDialog(clipboard: Clipboard): String {
        ctrlAnd(KeyEvent.VK_L)    // <---------------------------------------------- "Ctrl" + "L"
        delay(50)
        ctrlAnd(KeyEvent.VK_C)    // <---------------------------------------------- "Ctrl" + "C"
        delay(50)

        val baseUrl = try {clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""}
        catch (e: Exception) {println("Erreur de récupération d'URL : $e"); ""}
        val newUrl = if (baseUrl.isNotBlank() && !baseUrl.contains("overlay/contact-info/")) {if (baseUrl.endsWith("/")) baseUrl + "overlay/contact-info/" else "$baseUrl/overlay/contact-info/"} else baseUrl
        val clipboardContent = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        val stringSelection = java.awt.datatransfer.StringSelection(newUrl)
        clipboardContent.setContents(stringSelection, null)

        ctrlAnd(KeyEvent.VK_V)    // <---------------------------------------------- "Ctrl" + "V"
        delay(50)
        robot.keyPress(KeyEvent.VK_ENTER)      // <--------------------------------- "Enter"
        robot.keyRelease(KeyEvent.VK_ENTER)    // <--------------------------------- "Enter"
        delay(50)
        return newUrl
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