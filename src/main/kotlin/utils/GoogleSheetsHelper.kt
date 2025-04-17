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
import com.madsky.linkedinscraper.generated.resources.Res
import config.GlobalInstance.config as gC
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.*
import java.security.GeneralSecurityException
import java.io.Serializable
import java.nio.file.*

object GoogleSheetsHelper {
    private const val APPLICATION_NAME = "LinkedInScraper"
    private val DEFAULT_HEADERS = listOf(listOf("SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN"))
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_READONLY)
    private val APP_DATA_DIR = Paths.get(System.getProperty("user.home"), ".LinkedInScraper")
    private val TOKENS_DIRECTORY = APP_DATA_DIR.resolve("tokens").toString()
    private const val LOCAL_SERVER_PORT = 8888
    private val httpTransport: NetHttpTransport by lazy {try {GoogleNetHttpTransport.newTrustedTransport()} catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur d'initialisation du transport HTTP : ${e.message}", ConsoleMessageType.ERROR); return@lazy NetHttpTransport()}}
    private val credentialManager: CredentialManager by lazy {
        try {Files.createDirectories(Paths.get(TOKENS_DIRECTORY))}
        catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("❌ Impossible de créer le dossier de tokens : ${e.message}", ConsoleMessageType.WARNING)}
        val clientSecrets = runBlocking {findClientSecrets()}
        val dataStoreFactory = FileDataStoreFactory(File(TOKENS_DIRECTORY))
        CredentialManager(httpTransport, clientSecrets, dataStoreFactory)
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun findClientSecrets(): GoogleClientSecrets? = runCatching {val bytes = Res.readBytes("files/client_secret.json"); GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(bytes.inputStream()))}.onFailure {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de la lecture de client_secret.json: ${it.message}", ConsoleMessageType.ERROR)}.getOrNull()
    @Volatile private var sheetsService: Sheets? = null
    @Volatile private var driveService: Drive? = null
    private val serviceInitMutex = Mutex()

    private class CredentialManager(
        private val transport: NetHttpTransport,
        private val clientSecrets: GoogleClientSecrets?,
        private val dataStoreFactory: FileDataStoreFactory
    ) {
        @Volatile private var currentCredential: Credential? = null

        private fun getValidOrRefreshed(cred: Credential, source: String): Credential? {
            if ((cred.expiresInSeconds ?: 0) > 60) return cred
            if (cred.refreshToken == null) return null
            return try {cred.refreshToken(); cred}
            catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("❌ Échec du rafraîchissement du token ($source) : ${e.message}", ConsoleMessageType.WARNING); null}
        }

        suspend fun getCredential(): Credential? = withContext(Dispatchers.IO) {
            currentCredential?.let {cachedCred -> getValidOrRefreshed(cachedCred, "cache")?.let {return@withContext it}}
            val flow = GoogleAuthorizationCodeFlow.Builder(transport, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(dataStoreFactory).setAccessType("offline").build()
            var loadedCred: Credential? = null
            try {loadedCred = flow.loadCredential("user")}
            catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur accès tokens stockés: ${e.message}", ConsoleMessageType.WARNING)}
            loadedCred?.let {loaded -> getValidOrRefreshed(loaded, "storage")?.let {validLoaded -> currentCredential = validLoaded; return@withContext validLoaded}; if (loaded.refreshToken != null) {clearCredentials()}}
            try {
                gC.consoleMessage.value = ConsoleMessage("⏳ Ouverture du navigateur pour l'authentification Google...", ConsoleMessageType.INFO)
                val receiver = LocalServerReceiver.Builder().setPort(LOCAL_SERVER_PORT).build()
                val newCredential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
                currentCredential = newCredential
                gC.consoleMessage.value = ConsoleMessage("✅ Authentification Google réussie.", ConsoleMessageType.SUCCESS)
                return@withContext newCredential
            }
            catch (e: Exception) {
                val errorMsg = e.message ?: "Erreur inconnue"
                gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de l'authentification Google : $errorMsg", ConsoleMessageType.ERROR)
                return@withContext null
            }
        }

        fun clearCredentials() {
            currentCredential = null
            try {
                val dataStore = dataStoreFactory.getDataStore<Serializable>("user")
                if (!dataStore.isEmpty) {dataStore.clear()}
            }
            catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("⚠️ Erreur lors de la suppression des tokens : ${e.message}", ConsoleMessageType.WARNING)}
            catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("⚠️ Erreur inattendue lors de la suppression des tokens : ${e.message}", ConsoleMessageType.WARNING)}

        }
    }

    suspend fun login(): Boolean {
        return try {credentialManager.getCredential(); gC.isLoggedIn.value = true; true}
        catch (e: IOException) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur d'écriture lors de la connexion : ${e.message}", ConsoleMessageType.ERROR); gC.isLoggedIn.value = false; false}
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
        gC.consoleMessage.value = ConsoleMessage("✅ Compte Google déconnecté avec succés.", ConsoleMessageType.INFO)
    }

    suspend fun getSheetsService(): Sheets? = withContext(Dispatchers.IO) {
        sheetsService?.let {return@withContext it}
        return@withContext serviceInitMutex.withLock {
            sheetsService ?: run {
                try {val credential = credentialManager.getCredential(); Sheets.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build().also {sheetsService = it}}
                catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur d'initialisation du service Sheets : ${e.message}", ConsoleMessageType.ERROR); return@withContext null}
            }
        }
    }

    suspend fun getDriveService(): Drive? = withContext(Dispatchers.IO) {
        driveService?.let {return@withContext it}
        return@withContext serviceInitMutex.withLock {
            driveService ?: run {
                try {val credential = credentialManager.getCredential(); Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build().also {driveService = it}}
                catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur d'initialisation du service Drive : ${e.message}", ConsoleMessageType.ERROR); return@withContext null}
            }
        }
    }

    suspend fun findSpreadsheets(): List<Pair<String, String>>? = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService()
            val result = service?.files()?.list()?.setQ("mimeType='application/vnd.google-apps.spreadsheet' and trashed=false")?.setFields("files(id, name)")?.setOrderBy("name")?.execute()
            val sheets = result?.files?.mapNotNull {it.id to it.name} ?: emptyList()
            if (sheets.isEmpty()) {gC.consoleMessage.value = ConsoleMessage("ℹ⚠\uFE0F Aucune feuille Google Sheets trouvée", ConsoleMessageType.INFO)}
            else {gC.consoleMessage.value = ConsoleMessage("✅ ${sheets.size} feuille(s) Google Sheets trouvée(s)", ConsoleMessageType.SUCCESS)}
            return@withContext sheets
        }
        catch (e: Exception) {val errorMessage = "❌ Erreur lors de la récupération des feuilles Google Sheets: ${e.message}"; gC.consoleMessage.value = ConsoleMessage(errorMessage, ConsoleMessageType.ERROR); null}
    }

    suspend fun createNewSheet(title: String): String? = withContext(Dispatchers.IO) {
        if (title.isBlank()) {gC.consoleMessage.value = ConsoleMessage("⚠️ Le titre de la feuille ne peut pas être vide", ConsoleMessageType.WARNING); return@withContext null}
        try {
            val service = getSheetsService()
            val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle(title))
            val initialSheet = Sheet().setProperties(SheetProperties().setTitle("Prospects"))
            spreadsheet.sheets = listOf(initialSheet)
            gC.consoleMessage.value = ConsoleMessage("⏳ Création de la feuille '$title'...", ConsoleMessageType.INFO)
            val response = service?.spreadsheets()?.create(spreadsheet)?.execute()
            val sheetId = response?.spreadsheetId
            val emptyRows = listOf(
                listOf(listOf("", "", "", "", "", "", "", "")),  // A1-H1
                listOf(listOf("", "", "", "", "", "", "", "")),  // A2-H2
                listOf(listOf("", "", "", "", "", "", "", ""))   // A3-H3
            )
            for (i in 0..2) {
                val emptyBody = ValueRange().setValues(emptyRows[i])
                service?.spreadsheets()?.values()?.update(sheetId, "Prospects!A${i+1}", emptyBody)?.setValueInputOption("RAW")?.execute()
            }
            val headers = listOf(listOf("", "SOCIETE", "PRENOM", "NOM", "POSTE", "EMAIL", "TEL", "LINKEDIN"))
            val headerBody = ValueRange().setValues(headers)
            service?.spreadsheets()?.values()?.update(sheetId, "Prospects!A4", headerBody)?.setValueInputOption("RAW")?.execute()
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
        try {
            val service = getSheetsService()
            val metadata = service?.spreadsheets()?.get(spreadsheetId)?.setFields("properties.title,sheets.properties.title")?.execute()
            return@withContext metadata
        }
        catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur de récupération des métadonnées. ($spreadsheetId): ${e.message}", ConsoleMessageType.ERROR); null}
    }

    suspend fun checkCredentialsFileExists(): Boolean {return try {findClientSecrets(); true} catch (e: Exception) {ConsoleMessage("❌ Erreur lors de la vérification du fichier de credentials : ${e.message}", ConsoleMessageType.ERROR); false}}
}