package utils

import kotlinx.coroutines.delay
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import config.GlobalInstance.config as gC

private const val CTRL_MODIFIER = KeyEvent.VK_CONTROL
private const val ALT_MODIFIER = KeyEvent.VK_ALT
private const val SHORT_DELAY_MS = 50L
private const val MEDIUM_DELAY_MS = 100L
private const val INITIAL_PAGE_LOAD_DELAY_MS = 1000L
private const val MAX_DETECTION_ATTEMPTS = 10
private const val PROFILE_MIN_CONTENT_LENGTH = 5000
private const val PROFILE_KEYWORD = "dialogue"

private val SUBSEQUENT_CHECK_DELAYS_MS = listOf(1000L, 2000L, 3000L)

class BotController {
    private val robot = Robot()
    private val systemClipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard

    private fun logMessage(message: String, type: ConsoleMessageType) {
        gC.consoleMessage.value = ConsoleMessage(message, type)
        if (type == ConsoleMessageType.WARNING || type == ConsoleMessageType.ERROR) {System.err.println(message)}
    }

    private suspend fun modifierAndKey(modifier: Int, key: Int, actionDelay: Long = SHORT_DELAY_MS) {
        robot.keyPress(modifier)
        robot.keyPress(key)
        delay(actionDelay)
        robot.keyRelease(key)
        robot.keyRelease(modifier)
        delay(actionDelay)
    }

    private suspend fun pressAndReleaseKey(key: Int, actionDelay: Long = SHORT_DELAY_MS) {
        robot.keyPress(key)
        delay(actionDelay)
        robot.keyRelease(key)
        delay(actionDelay)
    }

    private fun getClipboardText(): String {
        return try {if (systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {systemClipboard.getData(DataFlavor.stringFlavor) as? String ?: ""} else {""}}
        catch (e: Exception) {logMessage("Erreur lors de la lecture du presse-papiers: ${e.message}", ConsoleMessageType.WARNING); ""}
    }

    private fun setClipboardText(text: String) {
        try {val stringSelection = StringSelection(text); systemClipboard.setContents(stringSelection, null)}
        catch (e: Exception) {logMessage("Erreur lors de la définition du presse-papiers: ${e.message}", ConsoleMessageType.WARNING)}
    }

    private suspend fun copyPageContent(): String {
        modifierAndKey(CTRL_MODIFIER, KeyEvent.VK_A)
        delay(MEDIUM_DELAY_MS)
        modifierAndKey(CTRL_MODIFIER, KeyEvent.VK_C)
        delay(MEDIUM_DELAY_MS)
        return getClipboardText()
    }

    private fun isContentLikelyProfile(content: String): Boolean {
        if (content.length < PROFILE_MIN_CONTENT_LENGTH) return false
        return content.lines().filter {it.isNotBlank()}.take(5).any {it.contains(PROFILE_KEYWORD, true)}
    }

    private suspend fun navigateToContactInfoPage(): String {
        modifierAndKey(CTRL_MODIFIER, KeyEvent.VK_L)
        delay(MEDIUM_DELAY_MS)
        modifierAndKey(CTRL_MODIFIER, KeyEvent.VK_C)
        delay(MEDIUM_DELAY_MS)
        val baseUrl = getClipboardText()
        val newUrlCandidate = if (baseUrl.isBlank()) {logMessage("URL de base non récupérée", ConsoleMessageType.WARNING); ""}
        else if (baseUrl.contains("overlay/contact-info/", ignoreCase = true)) {baseUrl}
        else {"${baseUrl.removeSuffix("/")}/overlay/contact-info/"}
        if (newUrlCandidate.isNotBlank()) {
            setClipboardText(newUrlCandidate)
            delay(MEDIUM_DELAY_MS)
            modifierAndKey(CTRL_MODIFIER, KeyEvent.VK_V)
            delay(MEDIUM_DELAY_MS)
            pressAndReleaseKey(KeyEvent.VK_ENTER)
        }
        return newUrlCandidate
    }

    private suspend fun checkContentAndReport(successMessage: String, onNewClipboardContent: (String) -> Unit): Boolean {
        val clipboardContent = copyPageContent()
        if (isContentLikelyProfile(clipboardContent)) {
            logMessage(successMessage, ConsoleMessageType.SUCCESS)
            onNewClipboardContent(clipboardContent)
            return true
        }
        return false
    }

    suspend fun detect(onNewClipboardContent: (String) -> Unit): Boolean {
        logMessage("ℹ️ Vérification initiale de la page...", ConsoleMessageType.INFO)
        if (checkContentAndReport("✅ Profil détecté sur la page actuelle.", onNewClipboardContent)) {return true}
        for (attempt in 1..MAX_DETECTION_ATTEMPTS) {
            logMessage("⏳ Tentative de navigation et détection $attempt/$MAX_DETECTION_ATTEMPTS...", ConsoleMessageType.INFO)
            navigateToContactInfoPage()
            delay(INITIAL_PAGE_LOAD_DELAY_MS)
            if (checkContentAndReport("✅ Profil détecté après navigation (Tentative $attempt).", onNewClipboardContent)) {return true}
            for (extraDelayMs in SUBSEQUENT_CHECK_DELAYS_MS) {
                logMessage("⏳ Attente supplémentaire ($extraDelayMs ms) et re-vérification (Tentative $attempt)...", ConsoleMessageType.INFO)
                delay(extraDelayMs)
                if (checkContentAndReport("✅ Profil détecté après attente supplémentaire (Tentative $attempt).", onNewClipboardContent)) {return true}
            }
        }
        logMessage("❌ La page de profil n'a pas été détectée après $MAX_DETECTION_ATTEMPTS tentatives.", ConsoleMessageType.ERROR)
        return false
    }

    suspend fun changeApp() {modifierAndKey(ALT_MODIFIER, KeyEvent.VK_TAB, actionDelay = 100L)}
}