package pt.ipt.arcanedex_app.data.models.User

data class RegisterRequest(
    val FirstName: String,
    val LastName: String,
    val Email: String,
    val Genero: String,
    val Username: String,
    val Password: String,
    val Role: String
)
