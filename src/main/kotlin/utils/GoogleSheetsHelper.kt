package utils

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.*
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.*
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.*
import com.google.api.services.sheets.v4.*
import com.google.api.services.sheets.v4.model.*
import config.GlobalInstance.config as gC
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import java.io.*
import java.security.GeneralSecurityException
import java.io.Serializable

object GoogleSheetsHelper {
    private const val APPLICATION_NAME = "LinkedInScraper"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_READONLY)
    private val TOKENS_DIRECTORY_PATH = File(System.getProperty("user.home"), ".LinkedInScraper/tokens").absolutePath
    private const val CREDENTIALS_FILE_PATH = "/files/client_secret.json"
    private const val LOCAL_SERVER_PORT = 8888
    private val httpTransport: NetHttpTransport by lazy {GoogleNetHttpTransport.newTrustedTransport()}
    private val credentialManager: CredentialManager by lazy {
        val clientSecretInputStream = GoogleSheetsHelper::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH) ?: throw FileNotFoundException("Fichier credentials introuvable: $CREDENTIALS_FILE_PATH")
        clientSecretInputStream.use {inputStream ->
            val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))
            val dataStoreFactory = FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH))
            CredentialManager(httpTransport, clientSecrets, dataStoreFactory)
        }
    }

    @Volatile private var sheetsService: Sheets? = null
    @Volatile private var driveService: Drive? = null

    private val serviceInitMutex = Mutex()

    private class CredentialManager(
        private val transport: NetHttpTransport,
        private val clientSecrets: GoogleClientSecrets,
        private val dataStoreFactory: FileDataStoreFactory
    ) {
        @Volatile private var currentCredential: Credential? = null
        suspend fun getCredential(): Credential = withContext(Dispatchers.IO) {
            currentCredential?.let {
                if ((it.expiresInSeconds ?: 0) > 60) {return@withContext it}
                else if (it.refreshToken != null) {
                    try {it.refreshToken(); return@withContext it}
                    catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("⚠️ Échec du rafraîchissement du token: ${e.message}", ConsoleMessageType.WARNING)}
                }
            }
            val loadedCredential = loadExistingCredential()
            if (loadedCredential != null) {currentCredential = loadedCredential; return@withContext loadedCredential}
            val flow = GoogleAuthorizationCodeFlow.Builder(transport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(dataStoreFactory).setAccessType("offline").build()
            val receiver = LocalServerReceiver.Builder().setPort(LOCAL_SERVER_PORT).build()
            try {
                gC.consoleMessage.value = ConsoleMessage("⏳ Ouverture du navigateur pour l'authentification Google...", ConsoleMessageType.INFO)
                val newCredential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user") // "user" est l'ID utilisateur pour le DataStore
                currentCredential = newCredential // Met à jour le cache mémoire
                gC.consoleMessage.value = ConsoleMessage("✅ Authentification Google réussie.", ConsoleMessageType.SUCCESS)
                return@withContext newCredential
            }
            catch (e: Exception) {
                val errorMessage = "❌ Erreur lors de l'authentification Google : ${e.message}"
                gC.consoleMessage.value = ConsoleMessage(errorMessage, ConsoleMessageType.ERROR)
                throw IOException("Échec de l'autorisation Google", e) // Propage l'erreur
            }
        }

        private fun loadExistingCredential(): Credential? {
            return try {
                val flow = GoogleAuthorizationCodeFlow.Builder(transport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(dataStoreFactory).setAccessType("offline").build()
                val credential = flow.loadCredential("user")
                credential?.takeIf {(it.accessToken != null && (it.expiresInSeconds ?: 0) > 60) || it.refreshToken != null}

            }
            catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("⚠️ Erreur accès tokens stockés: ${e.message}", ConsoleMessageType.WARNING); null}
        }

        fun clearCredentials() {
            currentCredential = null
            val dataStoreDir = File(TOKENS_DIRECTORY_PATH)
            if (dataStoreDir.exists()) {
                try {
                    val dataStore = dataStoreFactory.getDataStore<Serializable>("user")
                    dataStore.clear()
                    gC.consoleMessage.value = ConsoleMessage("ℹ️ Tokens d'authentification Google effacés.", ConsoleMessageType.INFO)
                }
                catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("⚠️ Erreur lors de la suppression des tokens : ${e.message}", ConsoleMessageType.WARNING)}
            }
        }
    }

    suspend fun login(): Boolean {
        return try {credentialManager.getCredential(); gC.isLoggedIn.value = true; true}
        catch (e: IOException) {gC.isLoggedIn.value = false; false}
        catch (e: GeneralSecurityException) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur de sécurité lors de la connexion : ${e.message}", ConsoleMessageType.ERROR); gC.isLoggedIn.value = false; false}
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur inconnue lors de la connexion : ${e.message}", ConsoleMessageType.ERROR); gC.isLoggedIn.value = false; false}
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        credentialManager.clearCredentials()
        sheetsService = null
        driveService = null
        gC.isLoggedIn.value = false
        gC.googleSheetsId.value = ""
        gC.sheetsFileName.value = ""
        gC.availableSheets.value = emptyList()
    }

    suspend fun getSheetsService(): Sheets = withContext(Dispatchers.IO) {
        sheetsService?.let {return@withContext it}
        return@withContext serviceInitMutex.withLock {sheetsService ?: run {
            val credential = credentialManager.getCredential()
            Sheets.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build().also {sheetsService = it}
        }}
    }

    suspend fun getDriveService(): Drive = withContext(Dispatchers.IO) {
        driveService?.let {return@withContext it}
        return@withContext serviceInitMutex.withLock {driveService ?: run {
            val credential = credentialManager.getCredential()
            Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build().also {driveService = it}
        }}
    }

    suspend fun findSpreadsheets(): List<Pair<String, String>>? = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService()
            val result = service.files().list().setQ("mimeType='application/vnd.google-apps.spreadsheet' and trashed=false").setFields("files(id, name)").setOrderBy("name").execute()
            result.files?.mapNotNull {it.id to it.name} ?: emptyList()
        }
        catch (e: Exception) {val errorMessage = "❌ Erreur lors de la récupération des feuilles Google Sheets: ${e.message}"; gC.consoleMessage.value = ConsoleMessage(errorMessage, ConsoleMessageType.ERROR); null}
    }

    suspend fun createNewSheet(title: String): String? = withContext(Dispatchers.IO) {
        try {
            val service = getSheetsService()
            val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(title))
            val initialSheet = Sheet().setProperties(SheetProperties().setTitle("Prospects"))
            spreadsheet.sheets = listOf(initialSheet)
            val response = service.spreadsheets().create(spreadsheet).execute()
            val sheetId = response.spreadsheetId
            val headers = listOf(listOf("SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN")) // En-têtes standards
            val headerBody = ValueRange().setValues(headers)
            service.spreadsheets().values().update(sheetId, "Prospects!A1", headerBody).setValueInputOption("RAW").execute()
            gC.consoleMessage.value = ConsoleMessage("✅ Feuille '$title' créée avec succès.", ConsoleMessageType.SUCCESS)
            return@withContext sheetId
        }
        catch (e: Exception) {
            val errorMessage = "❌ Erreur lors de la création de la feuille '$title': ${e.message}"
            gC.consoleMessage.value = ConsoleMessage(errorMessage, ConsoleMessageType.ERROR)
            return@withContext null
        }
    }

    suspend fun getSheetMetadata(spreadsheetId: String): Spreadsheet? = withContext(Dispatchers.IO) {
        if (spreadsheetId.isBlank()) return@withContext null
        try {val service = getSheetsService(); service.spreadsheets().get(spreadsheetId).setFields("properties.title,sheets.properties.title").execute()}
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur récupération métadonnées ($spreadsheetId): ${e.message}", ConsoleMessageType.ERROR); null}
    }
}