package manager

class GenerationManager {
    private val accentReplacements = mapOf(Regex("[éèêë]") to "e", Regex("[àâä]") to "a", Regex("[ùûü]") to "u", Regex("[îï]") to "i", Regex("[ôö]") to "o", Regex("[ç]") to "c")
    private val companySpecificEmailFormats = mapOf(
        "orange" to listOf("{first}.{last}@orange.com", "{first}.{last}@orange.fr"),
        "bnp" to listOf("{first}.{last}@bnpparibas.com", "{f_initial}{last}@bnpparibas.com"),
        "societe generale" to listOf("{first}.{last}@socgen.com", "{f_initial}{last}@socgen.com"),
        "total" to listOf("{first}.{last}@totalenergies.com", "{first}.{last}@total.com"),
        "carrefour" to listOf("{first}.{last}@carrefour.com", "{f_initial}{last}@carrefour.com"),
        "axa" to listOf("{first}.{last}@axa.com", "{first}.{last}@axa.fr"),
        "edf" to listOf("{first}.{last}@edf.fr", "{f_initial}{last}@edf.fr"),
        "engie" to listOf("{first}.{last}@engie.com", "{f_initial}{last}@engie.com"),
        "sncf" to listOf("{first}.{last}@sncf.fr", "{f_initial}.{last}@sncf.fr"),
        "renault" to listOf("{first}.{last}@renault.com", "{f_initial}{last}@renault.com"),
        "psa" to listOf("{first}.{last}@stellantis.com", "{first}.{last}@mpsa.com"),
        "lvmh" to listOf("{first}.{last}@lvmh.com", "{f_initial}{last}@lvmh.com"),
        "loreal" to listOf("{first}.{last}@loreal.com", "{f_initial}{last}@loreal.com"),
        "danone" to listOf("{first}.{last}@danone.com", "{f_initial}{last}@danone.com"),
        "michelin" to listOf("{first}.{last}@michelin.com", "{f_initial}{last}@michelin.com"),
        "capgemini" to listOf("{first}.{last}@capgemini.com", "{f_initial}.{last}@capgemini.com"),
        "atos" to listOf("{first}.{last}@atos.net", "{f_initial}{last}@atos.net"),
        "sopra" to listOf("{first}.{last}@soprasteria.com", "{f_initial}{last}@soprasteria.com"),
        "thales" to listOf("{first}.{last}@thalesgroup.com", "{f_initial}{last}@thalesgroup.com"),
        "airbus" to listOf("{first}.{last}@airbus.com", "{f_initial}{last}@airbus.com")
    )

    fun extractDomain(company: String): String {
        if (company.isBlank() || company == "Entreprise inconnue") {return "domaine_inconnu.com"}
        val cleanCompany = company.lowercase().replace(Regex("[^a-z0-9]"), "").replace(Regex("(inc|llc|ltd|sarl|sas|sa|eurl|group|groupe)$"), "")
        return "$cleanCompany.com"
    }

    fun generateEmails(firstName: String, middleName: String, lastName: String, domain: String, company: String): List<String> {
        if (firstName.isBlank() || lastName.isBlank() || domain.isBlank()) {return emptyList()}
        val cleanFirstName = normalizeText(firstName)
        val cleanMiddleName = normalizeText(middleName)
        val cleanLastName = normalizeText(lastName)
        val firstInitial = if (cleanFirstName.isNotEmpty()) cleanFirstName.first().toString() else ""
        val lastInitial = if (cleanLastName.isNotEmpty()) cleanLastName.first().toString() else ""
        val emailFormats = mutableListOf<String>()
        emailFormats.addAll(listOf(
            "$cleanFirstName.$cleanLastName@$domain",                                       // prenom.nom@domaine.com - très courant en France
            "$cleanFirstName@$domain",                                                      // prenom@domaine.com - courant pour les petites entreprises
            "$firstInitial$cleanLastName@$domain",                                          // pnom@domaine.com - assez courant
            "$cleanLastName.$cleanFirstName@$domain",                                       // nom.prenom@domaine.com - utilisé dans certaines entreprises
            "$cleanFirstName-$cleanLastName@$domain",                                       // prenom-nom@domaine.com
            "$cleanFirstName$cleanLastName@$domain",                                        // prenomnom@domaine.com
            "$cleanLastName$cleanFirstName@$domain",                                        // nomprenom@domaine.com
            "$firstInitial.$cleanLastName@$domain",                                         // p.nom@domaine.com
            "$cleanFirstName.$cleanLastName@${domain.replace(".com", ".fr")}"               // version .fr
        ))
        val lowercaseCompany = company.lowercase()
        companySpecificEmailFormats.forEach {(companyName, formats) -> if (lowercaseCompany.contains(companyName)) {formats.forEach {format -> emailFormats.add(format.replace("{first}", cleanFirstName).replace("{last}", cleanLastName).replace("{f_initial}", firstInitial).replace("{l_initial}", lastInitial))}}}
        if (lowercaseCompany.contains("freelance") || lowercaseCompany.contains("indépendant") || lowercaseCompany.contains("consultant") || lowercaseCompany.contains("auto-entrepreneur")) {
            emailFormats.addAll(listOf(
                "$cleanFirstName.$cleanLastName@gmail.com",
                "$cleanFirstName$cleanLastName@gmail.com",
                "$cleanFirstName.$cleanLastName@outlook.fr",
                "$cleanFirstName.$cleanLastName@outlook.com"
            ))
        }
        return emailFormats.distinct()
    }

    private fun normalizeText(text: String): String {
        var normalized = text.lowercase()
        accentReplacements.forEach {(regex, replacement) -> normalized = normalized.replace(regex, replacement)}
        normalized = normalized.replace(Regex("[^a-z0-9]"), "")
        return normalized
    }
}