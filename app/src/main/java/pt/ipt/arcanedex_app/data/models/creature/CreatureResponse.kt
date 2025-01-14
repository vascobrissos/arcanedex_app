package pt.ipt.arcanedex_app.data.models.creature

data class CreatureResponse(
    val data: List<Creature>,
    val count: Int,
    val totalCount: Int
)
