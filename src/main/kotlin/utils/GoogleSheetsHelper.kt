package utils

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.googleapis.auth.oauth2.*
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.*
import com.google.api.services.sheets.v4.*
import config.GlobalInstance.config as gC
import java.io.*
import kotlinx.coroutines.*

object GoogleSheetsHelper {
    private const val APPLICATION_NAME = "LinkedInScraper"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_METADATA_READONLY)
    private const val TOKENS_DIRECTORY_PATH = "tokens"
    private const val CLIENT_SECRET_FILE_PATH = "src/jvmMain/composeResources/file/client_secret.json"
    private const val LOCAL_SERVER_PORT = 8888
    private var sheetsService: Sheets? = null
    private var driveService: Drive? = null

    suspend fun getSheetsService(): Sheets = withContext(Dispatchers.IO) {
        if (sheetsService == null) {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val credentials = getCredentials(httpTransport)
            sheetsService = Sheets.Builder(httpTransport, JSON_FACTORY, credentials).setApplicationName(APPLICATION_NAME).build()
        }
        sheetsService!!
    }

    private suspend fun getDriveService(): Drive = withContext(Dispatchers.IO) {
        if (driveService == null) {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val credentials = getCredentials(httpTransport)
            driveService = Drive.Builder(httpTransport, JSON_FACTORY, credentials).setApplicationName(APPLICATION_NAME).build()
        }
        driveService!!
    }

    private suspend fun getCredentials(httpTransport: NetHttpTransport): Credential = withContext(Dispatchers.IO) {
        val clientSecretFile = File(CLIENT_SECRET_FILE_PATH)
        if (!clientSecretFile.exists()) {throw FileNotFoundException("Fichier client_secret introuvable : $CLIENT_SECRET_FILE_PATH")}
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(FileInputStream(clientSecretFile)))
        val flow = GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH))).setAccessType("offline").build()
        val credential = flow.loadCredential("user")
        if (credential != null && credential.refreshToken != null) {return@withContext credential}
        val receiver = com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver.Builder().setPort(LOCAL_SERVER_PORT).build()
        try {gC.consoleMessage.value = ConsoleMessage("⏳ Ouverture du navigateur pour l'authentification Google Sheets...", ConsoleMessageType.INFO); return@withContext AuthorizationCodeInstalledApp(flow, receiver).authorize("user")}
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de l'authentification Google Sheets : ${e.message}", ConsoleMessageType.ERROR); throw e}
    }

    suspend fun listAvailableSpreadsheets(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            val result = driveService.files().list().setQ("mimeType='application/vnd.google-apps.spreadsheet'").setFields("files(id, name)").execute()
            return@withContext result.files.map {it.id to it.name}
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de la récupération des feuilles : ${e.message}", ConsoleMessageType.ERROR); throw e}
    }

    suspend fun createNewSpreadsheet(title: String): String = withContext(Dispatchers.IO) {
        val service = getSheetsService()
        val spreadsheet = com.google.api.services.sheets.v4.model.Spreadsheet().setProperties(com.google.api.services.sheets.v4.model.SpreadsheetProperties().setTitle(title))
        val response = service.spreadsheets().create(spreadsheet).execute()
        val sheetId = response.spreadsheetId
        val headers = listOf(listOf("SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN"))
        val body = com.google.api.services.sheets.v4.model.ValueRange().setValues(headers)
        service.spreadsheets().values().update(sheetId, "A1", body).setValueInputOption("RAW").execute()
        return@withContext sheetId
    }

    suspend fun checkSheetAccess(spreadsheetId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            service.spreadsheets().get(spreadsheetId).execute()
            return@withContext true
        }
        catch (e: Exception) {return@withContext false}
    }
}