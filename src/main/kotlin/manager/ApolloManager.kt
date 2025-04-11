package manager

import data.ProspectData
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import config.GlobalInstance.config as gC

class ApolloManager {
    private val client = OkHttpClient()
    private val userAgentProvider = UserAgentManager()

    fun validateApolloApiKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        val client = OkHttpClient()
        val request = Request.Builder().url("https://api.apollo.io/api/v1/auth/health").addHeader("accept", "application/json").addHeader("Cache-Control", "no-cache").addHeader("Content-Type", "application/json").addHeader("x-api-key", apiKey).build()
        return try {client.newCall(request).execute().use {response -> response.isSuccessful}} catch (e: Exception) {false}
    }

    fun enrichProfileData(firstName: String, lastName: String, company: String, jobTitle: String, email: String): ProspectData {
        if (firstName.isBlank() || firstName == "Prénom inconnu" || lastName.isBlank() || lastName == "Nom de famille inconnu") {return ProspectData(company, "$firstName $lastName", firstName, "", lastName, jobTitle, email, "", "", emptyList())}
        val apolloData = fetchApolloData(firstName, lastName, company)
        if (apolloData == null) {return ProspectData(company, "$firstName $lastName", firstName, "", lastName, jobTitle, email, "", "", emptyList())}
        val personData = apolloData.optJSONObject("person")
        var updatedCompany = company
        var updatedJobTitle = jobTitle
        var updatedEmail = email

        if (personData != null) {
            if (company == "Entreprise inconnue") {
                val lastJobHistory = personData.optJSONArray("employment_history")?.optJSONObject(0)
                val apolloCompany = lastJobHistory?.optString("organization_name")
                if (!apolloCompany.isNullOrBlank()) {updatedCompany = apolloCompany}
            }
            if (jobTitle == "Poste inconnu") {
                val lastJobHistory = personData.optJSONArray("employment_history")?.optJSONObject(0)
                val apolloJobTitle = lastJobHistory?.optString("title")
                if (!apolloJobTitle.isNullOrBlank()) {updatedJobTitle = apolloJobTitle}
            }
            if (email == "Email inconnu") {
                val apolloEmail = personData.optString("email")
                if (apolloEmail.isNotBlank()) {updatedEmail = apolloEmail}
            }
        }
        return ProspectData(updatedCompany, "$firstName $lastName", firstName, "", lastName, updatedJobTitle, "", updatedEmail, "", emptyList())
    }

    private fun fetchApolloData(firstName: String, lastName: String, company: String): JSONObject? {
        if (firstName.isBlank() || lastName.isBlank()) {return null}
        try {
            val jsonBody = JSONObject().apply {
                put("first_name", firstName)
                put("last_name", lastName)
                put("organization_name", company)
                put("reveal_personal_emails", true)
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("https://api.apollo.io/api/v1/people/match").post(requestBody).addHeader("accept", "application/json").addHeader("Cache-Control", "no-cache").addHeader("Content-Type", "application/json").addHeader("User-Agent", userAgentProvider.getSmartUserAgent()).addHeader("x-api-key", gC.apiKey.value).build()
            client.newCall(request).execute().use {response ->
                if (!response.isSuccessful) {return null}
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {return null}
                return JSONObject(responseBody)
            }
        }
        catch (e: Exception) {return null}
    }
}