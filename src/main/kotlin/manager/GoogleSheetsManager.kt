package manager

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import config.GlobalInstance.config as gC
import kotlinx.coroutines.*
import utils.*

class GoogleSheetsManager {
    private val targetSheetName = "Prospects"
    private val retryCount = 3
    private val headerRange = "A1:L4"

    suspend fun addLineToGoogleSheets() {
        val spreadsheetId = gC.googleSheetsId.value
        if (spreadsheetId.isEmpty()) {gC.consoleMessage.value = ConsoleMessage("❌ ID Google Sheets non défini. Impossible d'ajouter.", ConsoleMessageType.ERROR); return}
        val prospect = gC.currentProfile.value ?: return
        withContext(Dispatchers.IO) {
            try {
                if (!gC.isLoggedIn.value && !GoogleSheetsHelper.login()) {gC.consoleMessage.value = ConsoleMessage("❌ Non connecté à Google. Impossible d'ajouter.", ConsoleMessageType.ERROR); return@withContext}
                var sheetsService = GoogleSheetsHelper.getSheetsService()
                val createdSheet = ensureSheetExists(sheetsService, spreadsheetId, targetSheetName)
                if (createdSheet) {writeHeaders(sheetsService, spreadsheetId, targetSheetName)}
                val firstEmptyRow = findFirstEmptyRow(sheetsService, spreadsheetId, targetSheetName)
                val data = listOf(listOf(prospect.company, prospect.firstName, prospect.lastName, prospect.jobTitle, prospect.email, prospect.phoneNumber, prospect.linkedinUrl))
                val range = "$targetSheetName!B$firstEmptyRow:L$firstEmptyRow"
                var success = false
                var lastError: Exception? = null
                for (attempt in 1..retryCount) {
                    try {
                        val body = ValueRange().setValues(data)
                        sheetsService?.spreadsheets()?.values()?.update(spreadsheetId, range, body)?.setValueInputOption("RAW")?.execute()
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
                            catch (_: Exception) {}
                        }
                        delay(1000)
                    }
                }
                if (success) {gC.consoleMessage.value = ConsoleMessage("✅ Données ajoutées à '$targetSheetName' à la ligne $firstEmptyRow.", ConsoleMessageType.SUCCESS)}
                else {gC.consoleMessage.value = ConsoleMessage("❌ Échec après $retryCount tentatives: ${lastError?.message}", ConsoleMessageType.ERROR)}
            }
            catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur ajout Google Sheets: ${e.message}", ConsoleMessageType.ERROR)}
        }
    }

    private suspend fun findFirstEmptyRow(service: Sheets?, spreadsheetId: String, sheetTitle: String): Int {
        val range = "$sheetTitle!B5:L1000"
        val response = service?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
        val values = response?.getValues() ?: emptyList()
        for ((index, row) in values.withIndex()) {if (row.size < 11 || row.all {it == "" || it == null}) {return 5 + index}}
        return 5 + values.size
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
            val headerResponse = service?.spreadsheets()?.values()?.get(spreadsheetId, "$sheetTitle!$headerRange")?.execute()
            val existingHeaders = headerResponse?.getValues()
            val headerFound = existingHeaders?.any {row -> row.size >= 7 && row.any {cell -> cell.toString().equals("SOCIETE", ignoreCase = true) || cell.toString().equals("PRENOM", ignoreCase = true) || cell.toString().equals("NOM", ignoreCase = true)}} == true
            if (!headerFound) {
                val headers = listOf(listOf("", "", "", "", "", "", ""))
                val headerBody1 = ValueRange().setValues(headers)
                service?.spreadsheets()?.values()?.update(spreadsheetId, "$sheetTitle!A1", headerBody1)?.setValueInputOption("RAW")?.execute()
                val headers2 = listOf(listOf("", "", "", "", "", "", ""))
                val headerBody2 = ValueRange().setValues(headers2)
                service?.spreadsheets()?.values()?.update(spreadsheetId, "$sheetTitle!A2", headerBody2)?.setValueInputOption("RAW")?.execute()
                val headers3 = listOf(listOf("", "", "", "", "", "", ""))
                val headerBody3 = ValueRange().setValues(headers3)
                service?.spreadsheets()?.values()?.update(spreadsheetId, "$sheetTitle!A3", headerBody3)?.setValueInputOption("RAW")?.execute()
                val actualHeaders = listOf(listOf("", "SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN"))
                val headerBody4 = ValueRange().setValues(actualHeaders)
                service?.spreadsheets()?.values()?.update(spreadsheetId, "$sheetTitle!A4", headerBody4)?.setValueInputOption("RAW")?.execute()
            }
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur écriture en-têtes '$sheetTitle': ${e.message}", ConsoleMessageType.ERROR)}
    }

    suspend fun selectGoogleSheetsFile(sheetsId: String) {
        if (sheetsId.isBlank()) {gC.consoleMessage.value = ConsoleMessage("⚠️ Tentative de sélection avec un ID vide.", ConsoleMessageType.WARNING); return}
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