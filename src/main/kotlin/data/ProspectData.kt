package data

import kotlinx.serialization.Serializable

@Serializable
data class ProspectData(
    val company: String = "",
    val fullName: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val linkedinUrl: String = "",
    val generatedEmails: List<String> = emptyList(),
)