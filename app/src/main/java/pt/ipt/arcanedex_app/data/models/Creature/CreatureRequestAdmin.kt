package pt.ipt.arcanedex_app.data.models.Creature

data class CreatureRequestAdmin(
    val Name: String,
    val Lore: String?,
    val Img: String? // Base64 encoded image
)