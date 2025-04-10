package manager

import data.ProspectData
import config.GlobalInstance.config as gC
import utils.*
import com.google.api.services.sheets.v4.model.*

class GoogleSheetsManager {
    suspend fun exportToGoogleSheets() {
        try {
            val spreadsheetId = gC.googleSheetsId.value
            println("yep")
            if (spreadsheetId.isEmpty()) {gC.consoleMessage.value = ConsoleMessage("❌ ID de feuille Google Sheets non configuré", ConsoleMessageType.ERROR); return}
            println("yep2")
            println(spreadsheetId.toString())
            val sheetsService = GoogleSheetsHelper.getSheetsService()
            val range = "Prospects!B2"
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            val sheetExists = spreadsheet.sheets.any {it.properties.title == "Prospects"}
            println("yep3")
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

    suspend fun importFromGoogleSheets(sheetsId: String): List<ProspectData> {
        gC.isImportationLoading.value = true
        try {
            val sheetsService = GoogleSheetsHelper.getSheetsService()
            val range = "Prospects!B2:H"
            val response = sheetsService.spreadsheets().values().get(sheetsId, range).execute()
            val values = response.getValues()
            if (values == null || values.isEmpty()) {gC.consoleMessage.value = ConsoleMessage("❌ Aucune donnée trouvée dans la feuille Google Sheets", ConsoleMessageType.ERROR); return emptyList()}
            val prospects = values.map {row ->
                ProspectData(
                    company = if (row.isNotEmpty()) row[0].toString() else "",
                    fullName = "",
                    firstName = if (row.size > 1) row[1].toString() else "",
                    middleName = "",
                    lastName = if (row.size > 2) row[2].toString() else "",
                    jobTitle = if (row.size > 3) row[3].toString() else "",
                    email = if (row.size > 4) row[4].toString() else "",
                    phoneNumber = if (row.size > 5) row[5].toString() else "",
                    linkedinUrl = if (row.size > 6) row[6].toString() else "",
                    generatedEmails = emptyList()
                )
            }
            if (prospects.isNotEmpty()) {gC.currentProfile.value = prospects.firstOrNull(); gC.consoleMessage.value = ConsoleMessage("✅ Importation de ${prospects.size} prospects depuis Google Sheets réussie", ConsoleMessageType.SUCCESS)}
            return prospects
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de l'importation depuis Google Sheets : ${e.message}", ConsoleMessageType.ERROR); return emptyList()}
        finally {gC.isImportationLoading.value = false}
    }
}