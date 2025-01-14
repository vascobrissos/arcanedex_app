package pt.ipt.arcanedex_app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arcanes")
data class ArcaneEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val img: String?, // Base64 string
    val lore: String
)
