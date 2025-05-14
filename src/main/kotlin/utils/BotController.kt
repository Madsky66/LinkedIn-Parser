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
        catch (e: Exception) {
            System.err.println("Erreur lors de la lecture du presse-papiers: ${e.message}")
            gC.consoleMessage.value = ConsoleMessage("Erreur presse-papiers: ${e.message}", ConsoleMessageType.WARNING)
            ""
        }
    }

    private fun setClipboardText(text: String) {
        try {val stringSelection = StringSelection(text); systemClipboard.setContents(stringSelection, null)}
        catch (e: Exception) {
            System.err.println("Erreur lors de la définition du presse-papiers: ${e.message}"); gC.consoleMessage.value = ConsoleMessage("Erreur écriture presse-papiers: ${e.message}", ConsoleMessageType.WARNING)}
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
        val newUrlCandidate: String
        if (baseUrl.isNotBlank()) {if (baseUrl.contains("overlay/contact-info/", true)) {newUrlCandidate = baseUrl} else {val strippedBaseUrl = baseUrl.removeSuffix("/"); newUrlCandidate = "$strippedBaseUrl/overlay/contact-info/"}}
        else {
            System.err.println("Impossible de récupérer l'URL de base depuis le presse-papiers.")
            gC.consoleMessage.value = ConsoleMessage("URL de base non récupérée", ConsoleMessageType.WARNING)
            newUrlCandidate = ""
        }
        if (newUrlCandidate.isNotBlank()) {
            setClipboardText(newUrlCandidate)
            delay(MEDIUM_DELAY_MS)
            modifierAndKey(CTRL_MODIFIER, KeyEvent.VK_V)
            delay(MEDIUM_DELAY_MS)
            pressAndReleaseKey(KeyEvent.VK_ENTER)
            delay(INITIAL_PAGE_LOAD_DELAY_MS)
        }
        return newUrlCandidate
    }

    suspend fun detect(onNewClipboardContent: (String) -> Unit): Boolean {
        gC.consoleMessage.value = ConsoleMessage("ℹ️ Vérification initiale de la page...", ConsoleMessageType.INFO)
        var clipboardContent = copyPageContent()
        if (isContentLikelyProfile(clipboardContent)) {
            gC.consoleMessage.value = ConsoleMessage("✅ Profil détecté sur la page actuelle.", ConsoleMessageType.SUCCESS)
            onNewClipboardContent(clipboardContent)
            return true
        }
        for (attempt in 1..MAX_DETECTION_ATTEMPTS) {
            gC.consoleMessage.value = ConsoleMessage("⏳ Tentative de navigation et détection $attempt/$MAX_DETECTION_ATTEMPTS...", ConsoleMessageType.INFO)
            navigateToContactInfoPage()
            delay(INITIAL_PAGE_LOAD_DELAY_MS)
            clipboardContent = copyPageContent()
            if (isContentLikelyProfile(clipboardContent)) {
                gC.consoleMessage.value = ConsoleMessage("✅ Profil détecté après navigation (Tentative $attempt).", ConsoleMessageType.SUCCESS)
                onNewClipboardContent(clipboardContent)
                return true
            }
            for (extraDelayMs in SUBSEQUENT_CHECK_DELAYS_MS) {
                gC.consoleMessage.value = ConsoleMessage("⏳ Attente supplémentaire ($extraDelayMs ms) et re-vérification (Tentative $attempt)...", ConsoleMessageType.INFO)
                delay(extraDelayMs)
                clipboardContent = copyPageContent()
                if (isContentLikelyProfile(clipboardContent)) {
                    gC.consoleMessage.value = ConsoleMessage("✅ Profil détecté après attente supplémentaire (Tentative $attempt).", ConsoleMessageType.SUCCESS)
                    onNewClipboardContent(clipboardContent)
                    return true
                }
            }
        }
        gC.consoleMessage.value = ConsoleMessage("❌ La page de profil n'a pas été détectée après $MAX_DETECTION_ATTEMPTS tentatives.", ConsoleMessageType.ERROR)
        return false
    }

    suspend fun changeApp() {modifierAndKey(ALT_MODIFIER, KeyEvent.VK_TAB, actionDelay = 100L)}
}