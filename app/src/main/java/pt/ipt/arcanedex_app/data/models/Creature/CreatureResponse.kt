package pt.ipt.arcanedex_app.data.models.Creature

import pt.ipt.arcanedex_app.data.models.Creature.Creature

data class CreatureResponse(
    val data: List<Creature>,
    val count: Int,
    val totalCount: Int
)
