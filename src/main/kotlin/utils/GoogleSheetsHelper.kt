package utils

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import config.GlobalInstance.config as gC
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.*
import com.google.api.services.drive.*
import com.google.api.services.sheets.v4.*
import kotlinx.coroutines.*
import java.io.*
import java.security.GeneralSecurityException

object GoogleSheetsHelper {
    private const val APPLICATION_NAME = "LinkedInScraper"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_METADATA_READONLY)
    private const val TOKENS_DIRECTORY_PATH = "tokens"
    private const val CLIENT_SECRET_FILE_PATH = "src/main/composeResources/files/client_secret.json"
    private const val LOCAL_SERVER_PORT = 8888
    private var sheetsService: Sheets? = null
    private var driveService: Drive? = null

    suspend fun login(): Boolean = withContext(Dispatchers.IO) {
        try {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val credential = getCredentials(httpTransport)
            if (credential != null) {
                gC.isLoggedIn.value = true
                return@withContext true
            }
            false
        }
        catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur d'entrée/sortie lors de la connexion : ${e.message}", ConsoleMessageType.ERROR); false}
        catch (e: GeneralSecurityException) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur de sécurité lors de la connexion : ${e.message}", ConsoleMessageType.ERROR); false}
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur inconnue lors de la connexion : ${e.message}", ConsoleMessageType.ERROR); false}
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val tokensDir = File(TOKENS_DIRECTORY_PATH)
        if (tokensDir.exists()) tokensDir.deleteRecursively()
        gC.isLoggedIn.value = false
        gC.googleSheetsId.value = ""
        sheetsService = null
        driveService = null
    }

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
        if (!clientSecretFile.exists()) {val errorMessage = "Fichier client_secret introuvable : $CLIENT_SECRET_FILE_PATH"; gC.consoleMessage.value = ConsoleMessage("❌ $errorMessage", ConsoleMessageType.ERROR); throw FileNotFoundException(errorMessage)}
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(FileInputStream(clientSecretFile)))
        val flow = GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH))).setAccessType("offline").build()
        val credential = flow.loadCredential("user")
        if (credential != null && credential.refreshToken != null) {return@withContext credential}
        val receiver = LocalServerReceiver.Builder().setPort(LOCAL_SERVER_PORT).build()
        try {gC.consoleMessage.value = ConsoleMessage("⏳ Ouverture du navigateur pour l'authentification Google Sheets...", ConsoleMessageType.INFO); return@withContext AuthorizationCodeInstalledApp(flow, receiver).authorize("user")}
        catch (e: Exception) {val errorMessage = "❌ Erreur lors de l'authentification Google Sheets : ${e.message}"; gC.consoleMessage.value = ConsoleMessage(errorMessage, ConsoleMessageType.ERROR); throw Exception(errorMessage, e)}
    }

    suspend fun loadAvailableSheets() {
        gC.isImportationLoading.value = true
        try {gC.availableSheets.value = listAvailableSheets()}
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors du chargement des feuilles : ${e.message}", ConsoleMessageType.ERROR); throw e}
        finally {gC.isImportationLoading.value = false}
    }

    suspend fun listAvailableSheets(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()
            val result = driveService.files().list().setQ("mimeType='application/vnd.google-apps.spreadsheet'").setFields("files(id, name)").execute()
            return@withContext result.files.map {it.id to it.name}
        }
        catch (e: Exception) {val errorMessage = "❌ Erreur lors de la récupération des feuilles : ${e.message}"; gC.consoleMessage.value = ConsoleMessage(errorMessage, ConsoleMessageType.ERROR); throw Exception(errorMessage, e)}
    }

    suspend fun createNewSheet(title: String): String = withContext(Dispatchers.IO) {
        val service = getSheetsService()
        val spreadsheet = com.google.api.services.sheets.v4.model.Spreadsheet().setProperties(com.google.api.services.sheets.v4.model.SpreadsheetProperties().setTitle(title))
        val response = service.spreadsheets().create(spreadsheet).execute()
        val sheetId = response.spreadsheetId
        val headers = listOf(listOf("SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN"))
        val body = com.google.api.services.sheets.v4.model.ValueRange().setValues(headers)
        service.spreadsheets().values().update(sheetId, "A1", body).setValueInputOption("RAW").execute()
        return@withContext sheetId
    }

    suspend fun getSheetName(spreadsheetId: String): String? = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            return@withContext spreadsheet.properties.title
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de la récupération du nom de la feuille : ${e.message}", ConsoleMessageType.ERROR); return@withContext null}
    }

    suspend fun checkSheetAccess(spreadsheetId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            service.spreadsheets().get(spreadsheetId).execute()
            return@withContext true
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de la vérification de l'accès à la feuille : ${e.message}", ConsoleMessageType.ERROR); return@withContext false}
    }
}