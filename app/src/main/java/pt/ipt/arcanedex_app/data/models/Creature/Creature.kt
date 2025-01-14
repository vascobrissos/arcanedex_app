package pt.ipt.arcanedex_app.data.models

data class Creature(
    val Id: Int,
    val Name: String,
    val Img: String?,
    val Lore: String,
    var isFavoriteToUser: Boolean
)
