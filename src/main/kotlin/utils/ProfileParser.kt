package utils

import config.GlobalInstance.config as gC
import data.ProspectData
import kotlinx.coroutines.*
import manager.ApolloManager
import manager.GenerationManager

class ProfileParser {
    private val apolloManager = ApolloManager()
    private val generationManager = GenerationManager()
    private val excludePatterns = listOf("otification", "contenu", "rofil", "echerche", "accourcis", "menu", "ccueil", "éseau", "mplois", "essagerie", "Vous", "Pour les entreprises", "Premium", "Image", "relation", "Le statut est accessible", "clavier", "nouvelles", "actualité", "test", "Coordonnées", "Voir le profil complet", "Connexions", "Abonné", "Abonnés", "Voir tous les articles", "objectifs", "Plus", "détails")
    private val linkedInUrlPattern = Regex("(https?://)?(www\\.)?linkedin\\.com/in/[\\w-]+(/)?")
    private val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val phoneNumberPattern = Regex("(0|\\+33|0033)[1-9]([-. ]?[0-9]{2}){4}")

    fun processInput(applicationScope: CoroutineScope, input: String) {
        applicationScope.launch {
            gC.isExtractionLoading.value = true
            gC.consoleMessage.value = ConsoleMessage("⏳ Extraction des informations en cours...", ConsoleMessageType.INFO)
            val newProfile = extractProfileData(input)
            gC.currentProfile.value = newProfile
            gC.consoleMessage.value = when {
                newProfile.fullName.isBlank() || (newProfile.firstName == "Prénom inconnu" && newProfile.lastName == "Nom de famille inconnu") -> ConsoleMessage("❌ Aucune information traitable ou format du texte copié incorrect", ConsoleMessageType.ERROR)
                newProfile.firstName == "Prénom inconnu" || newProfile.lastName == "Nom de famille inconnu" -> ConsoleMessage("⚠️ Extraction des données incomplète", ConsoleMessageType.WARNING)
                else -> ConsoleMessage("✅ Extraction des informations réussie", ConsoleMessageType.SUCCESS)
            }
            gC.isExtractionLoading.value = false
        }
    }

    fun extractProfileData(text: String): ProspectData {
        if (text.isBlank()) return emptyProspectData()
        return try {
            val lines = text.split("\n").map {it.trim()}.filter {it.isNotEmpty()}.filterNot {line -> excludePatterns.any {pattern -> line.contains(pattern, ignoreCase = true)}}
            val fullNameLine = lines.firstOrNull() ?: ""
            val fullNameParts = fullNameLine.split(" ").filter {it.isNotEmpty()}
            val firstName = fullNameParts.firstOrNull() ?: "Prénom inconnu"
            val middleName = if (fullNameParts.size > 2) fullNameParts.subList(1, fullNameParts.size - 1).joinToString(" ") else ""
            val lastName = if (fullNameParts.size > 1) fullNameParts.last() else "Nom de famille inconnu"
            val jobTitle = extractJobTitle(lines) ?: "Poste inconnu"
            val company = extractCompany(lines, jobTitle) ?: "Entreprise inconnue"
            val email = extractEmail(lines) ?: "Email inconnu"
            val phoneNumber = extractPhoneNumber(lines) ?: ""
            val linkedinUrl = extractLinkedInUrl(lines) ?: "Url inconnu"
            val basicData = ProspectData(company, "$firstName $lastName", firstName, middleName, lastName, jobTitle, email, phoneNumber, linkedinUrl)
            val enrichedData = apolloManager.enrichProfileData(basicData.firstName, basicData.lastName, basicData.company, basicData.jobTitle, basicData.email)
            val domain = generationManager.extractDomain(enrichedData.company)
            val generatedEmails = generationManager.generateEmails(enrichedData.firstName, enrichedData.middleName, enrichedData.lastName, domain, enrichedData.company).distinct().toMutableList()
            if (enrichedData.email != "Email inconnu" && !generatedEmails.contains(enrichedData.email)) {generatedEmails.add(0, enrichedData.email)}
            enrichedData.copy(generatedEmails = generatedEmails, email = if (enrichedData.email == "Email inconnu" && generatedEmails.isNotEmpty()) generatedEmails.first() else enrichedData.email)
        }
        catch (e: Exception) {e.printStackTrace(); emptyProspectData()}
    }
    private fun extractLinkedInUrl(lines: List<String>): String? {return lines.find {it.matches(linkedInUrlPattern)}}
    private fun extractJobTitle(lines: List<String>): String? {
        val jobTitleIndex = lines.indexOfFirst {it.contains("ExpérienceExpérience")}
        if (jobTitleIndex != -1 && jobTitleIndex + 1 < lines.size) {return lines[jobTitleIndex + 1]}
        val commonTitles = listOf("CEO", "CTO", "CFO", "COO", "Directeur", "Manager", "Ingénieur", "Développeur", "Consultant")
        return lines.find {line -> commonTitles.any {title -> line.contains(title, true)}}
    }
    private fun extractCompany(lines: List<String>, jobTitle: String): String? {
        val companyIndex = if (jobTitle != "" && jobTitle != "Poste Inconnu") {lines.indexOfFirst {it.contains(jobTitle)}} else {lines.indexOfFirst {it.contains("chez", true)}}
        if (companyIndex != -1 && companyIndex + 5 < lines.size) {
            var loopIterations = 1
            var tempCompany = lines[companyIndex + loopIterations]
            while ((loopIterations < 6) && (tempCompany == jobTitle)) {
                loopIterations ++
                tempCompany = lines[companyIndex + loopIterations]
            }
            if ((companyIndex >= 6) && (tempCompany == jobTitle)) {return null}
            return lines[companyIndex + 1]
        }
        return null
    }
    private fun extractEmail(lines: List<String>): String? {lines.forEach {line -> val match = emailPattern.find(line); if (match != null) {return match.value}}; return null}
    private fun extractPhoneNumber(lines: List<String>): String? {lines.forEach {line -> val match = phoneNumberPattern.find(line); if (match != null) {return match.value}}; return null}
    companion object {fun emptyProspectData(): ProspectData = ProspectData("Entreprise inconnue", "Nom inconnu", "Prénom inconnu", "", "Nom de famille inconnu", "Poste inconnu", "", "", "")}
}