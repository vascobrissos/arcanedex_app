package pt.ipt.arcanedex_app.data.models.User

data class UserProfileRequest(
    val FirstName: String,
    val LastName: String,
    val Email: String,
    val Genero: String,
    val Password: String? = null
)