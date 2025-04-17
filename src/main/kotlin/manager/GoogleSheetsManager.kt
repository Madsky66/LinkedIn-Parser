package manager

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import config.GlobalInstance.config as gC
import kotlinx.coroutines.*
import utils.*

class GoogleSheetsManager {
    private val targetSheetName = "Prospects"
    private val retryCount = 3

    suspend fun addLineToGoogleSheets() {
        val spreadsheetId = gC.googleSheetsId.value
        if (spreadsheetId.isEmpty()) {gC.consoleMessage.value = ConsoleMessage("❌ ID Google Sheets non défini. Impossible d'ajouter.", ConsoleMessageType.ERROR); return}
        val prospect = gC.currentProfile.value
        if (prospect == null) {return}
        withContext(Dispatchers.IO) {
            try {
                if (!gC.isLoggedIn.value && !GoogleSheetsHelper.login()) {gC.consoleMessage.value = ConsoleMessage("❌ Non connecté à Google. Impossible d'ajouter.", ConsoleMessageType.ERROR); return@withContext}
                var sheetsService = GoogleSheetsHelper.getSheetsService()
                val createdSheet = ensureSheetExists(sheetsService, spreadsheetId, targetSheetName)
                if (createdSheet) {writeHeaders(sheetsService, spreadsheetId, targetSheetName)}
                val values = listOf(listOf(prospect.company, prospect.firstName, prospect.lastName, prospect.jobTitle, prospect.email, prospect.phoneNumber, prospect.linkedinUrl))
                val body = ValueRange().setValues(values)
                val appendRange = "$targetSheetName!A:A"
                var success = false
                var lastError: Exception? = null
                for (attempt in 1..retryCount) {
                    try {
                        sheetsService?.spreadsheets()?.values()?.append(spreadsheetId, appendRange, body)?.setValueInputOption("RAW")?.setInsertDataOption("INSERT_ROWS")?.execute()
                        success = true
                        break
                    }
                    catch (e: Exception) {
                        lastError = e
                        gC.consoleMessage.value = ConsoleMessage("⚠️ Tentative $attempt échouée: ${e.message}", ConsoleMessageType.WARNING)
                        if (e.message?.contains("401", ignoreCase = true) == true) {
                            try {
                                GoogleSheetsHelper.login()
                                sheetsService = GoogleSheetsHelper.getSheetsService()
                            }
                            catch (refreshError: Exception) {}
                        }
                        delay(1000)
                    }
                }
                if (success) {gC.consoleMessage.value = ConsoleMessage("✅ Données ajoutées à '$targetSheetName'.", ConsoleMessageType.SUCCESS)}
                else {gC.consoleMessage.value = ConsoleMessage("❌ Échec après $retryCount tentatives: ${lastError?.message}", ConsoleMessageType.ERROR)}
            }
            catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur ajout Google Sheets: ${e.message}", ConsoleMessageType.ERROR)}
        }
    }

    private fun ensureSheetExists(service: Sheets?, spreadsheetId: String, sheetTitle: String): Boolean {
        try {
            val spreadsheet = service?.spreadsheets()?.get(spreadsheetId)?.setFields("sheets.properties.title")?.execute()
            val sheetExists = spreadsheet?.sheets?.any {it.properties?.title == sheetTitle} == true
            if (!sheetExists) {
                val addSheetRequest = AddSheetRequest().setProperties(SheetProperties().setTitle(sheetTitle))
                val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))
                service?.spreadsheets()?.batchUpdate(spreadsheetId, batchUpdateRequest)?.execute()
                return true
            }
            return false
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur vérification/création onglet '$sheetTitle': ${e.message}", ConsoleMessageType.ERROR); throw e}
    }

    private suspend fun writeHeaders(service: Sheets?, spreadsheetId: String, sheetTitle: String) {
        try {
            val headers = listOf(listOf("SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN"))
            val headerBody = ValueRange().setValues(headers)
            val headerRange = "$sheetTitle!A1"
            service?.spreadsheets()?.values()?.update(spreadsheetId, headerRange, headerBody)?.setValueInputOption("RAW")?.execute()
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur écriture en-têtes '$sheetTitle': ${e.message}", ConsoleMessageType.ERROR)}
    }

    suspend fun selectGoogleSheetsFile(sheetsId: String) {
        if (sheetsId.isBlank()) {gC.consoleMessage.value = ConsoleMessage("⚠️ Tentative de sélection avec un ID vide.", ConsoleMessageType.WARNING);  return}
        gC.isImportationLoading.value = true
        try {
            if (!gC.isLoggedIn.value && !GoogleSheetsHelper.login()) {gC.consoleMessage.value = ConsoleMessage("❌ Non connecté à Google. Impossible de sélectionner.", ConsoleMessageType.ERROR); return}
            val metadata = GoogleSheetsHelper.getSheetMetadata(sheetsId)
            if (metadata != null) {
                val sheetName = metadata.properties?.title ?: "Sans titre"
                gC.googleSheetsId.value = sheetsId
                gC.sheetsFileName.value = sheetName
                AppDataManager.saveAppData()
                gC.consoleMessage.value = ConsoleMessage("✅ Fichier \"$sheetName\" sélectionné.", ConsoleMessageType.SUCCESS)
            }
            else {gC.googleSheetsId.value = ""; gC.sheetsFileName.value = ""; AppDataManager.saveAppData(); gC.consoleMessage.value = ConsoleMessage("⚠️ Impossible d'accéder au fichier spécifié.", ConsoleMessageType.WARNING)}
        }
        catch (e: Exception) {
            gC.consoleMessage.value = ConsoleMessage("❌ Erreur sélection fichier: ${e.message}", ConsoleMessageType.ERROR)
            gC.googleSheetsId.value = ""
            gC.sheetsFileName.value = ""
            AppDataManager.saveAppData()
        }
        finally {gC.isImportationLoading.value = false}
    }

    suspend fun refreshAvailableSheets() {
        gC.isImportationLoading.value = true
        try {
            if (!gC.isLoggedIn.value && !GoogleSheetsHelper.login()) {gC.consoleMessage.value = ConsoleMessage("❌ Non connecté à Google. Impossible de rafraîchir.", ConsoleMessageType.ERROR); gC.availableSheets.value = emptyList(); return}
            val sheets = GoogleSheetsHelper.findSpreadsheets()
            gC.availableSheets.value = sheets ?: emptyList()
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur rafraîchissement liste: ${e.message}", ConsoleMessageType.ERROR); gC.availableSheets.value = emptyList()}
        finally {gC.isImportationLoading.value = false}
    }

    suspend fun createAndSelectSheet(title: String) {
        if (title.isBlank()) {gC.consoleMessage.value = ConsoleMessage("⚠️ Le titre de la feuille ne peut pas être vide.", ConsoleMessageType.WARNING); return}
        gC.isImportationLoading.value = true
        try {
            if (!gC.isLoggedIn.value && !GoogleSheetsHelper.login()) {gC.consoleMessage.value = ConsoleMessage("❌ Non connecté à Google. Impossible de créer.", ConsoleMessageType.ERROR); return}
            val newSheetId = GoogleSheetsHelper.createNewSheet(title)
            if (newSheetId != null) {selectGoogleSheetsFile(newSheetId); refreshAvailableSheets()}
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur création/sélection feuille: ${e.message}", ConsoleMessageType.ERROR)}
        finally {gC.isImportationLoading.value = false}
    }
    suspend fun checkCredentialsFileExists(): Boolean {return GoogleSheetsHelper.checkCredentialsFileExists()}
}