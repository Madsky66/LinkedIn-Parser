package manager

import config.GlobalInstance.config as gC
import utils.*
import com.google.api.services.sheets.v4.model.*

class GoogleSheetsManager {
    suspend fun addLineToGoogleSheets() {
        try {
            val spreadsheetId = gC.googleSheetsId.value
            if (spreadsheetId.isEmpty()) {gC.consoleMessage.value = ConsoleMessage("❌ ID de feuille Google Sheets non configuré", ConsoleMessageType.ERROR); return}
            val sheetsService = GoogleSheetsHelper.getSheetsService()
            val range = "Prospects!B2"
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            val sheetExists = spreadsheet.sheets.any {it.properties.title == "Prospects"}
            if (!sheetExists) {
                val addSheetRequest = AddSheetRequest().setProperties(SheetProperties().setTitle("Prospects"))
                val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
                val headers = listOf(listOf("SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN"))
                val headerBody = ValueRange().setValues(headers)
                sheetsService.spreadsheets().values().update(spreadsheetId, range, headerBody).setValueInputOption("RAW").execute()
            }
            val prospect = gC.currentProfile.value ?: return
            val values = listOf(listOf(prospect.company, prospect.firstName, prospect.lastName, prospect.jobTitle, prospect.email, prospect.phoneNumber, prospect.linkedinUrl))
            val body = ValueRange().setValues(values)
            sheetsService.spreadsheets().values().append(spreadsheetId, range, body).setValueInputOption("RAW").setInsertDataOption("INSERT_ROWS").execute()
            gC.consoleMessage.value = ConsoleMessage("✅ Données synchronisées avec Google Sheets.", ConsoleMessageType.SUCCESS)
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de la synchronisation avec Google Sheets : ${e.message}", ConsoleMessageType.ERROR)}
    }

    suspend fun selectGoogleSheetsFile(sheetsId: String) {
        gC.isImportationLoading.value = true
        try {
            if (!GoogleSheetsHelper.checkSheetAccess(sheetsId)) {
                gC.consoleMessage.value = ConsoleMessage("❌ Impossible d'accéder à cette feuille Google Sheets", ConsoleMessageType.ERROR)
                return
            }
            val sheetName = GoogleSheetsHelper.getSheetName(sheetsId)
            gC.googleSheetsId.value = sheetsId
            gC.sheetsFileName.value = sheetName.toString()
            AppDataManager.saveAppData()
            gC.consoleMessage.value = ConsoleMessage("✅ Feuille Google Sheets \"$sheetName\" sélectionnée avec succès", ConsoleMessageType.SUCCESS)
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de l'importation du fichier Google Sheets : ${e.message}", ConsoleMessageType.ERROR)}
        finally {gC.isImportationLoading.value = false}
    }
}