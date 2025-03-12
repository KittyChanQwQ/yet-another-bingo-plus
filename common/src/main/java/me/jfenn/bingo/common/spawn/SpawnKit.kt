package me.jfenn.bingo.common.spawn

import kotlinx.serialization.Serializable
import me.jfenn.bingo.platform.item.IItemStackSerialized

@Serializable
data class SpawnKit(
    var items: List<IItemStackSerialized>,
) {
    companion object {
        val EMPTY get() = SpawnKit(emptyList())
    }
}

class SpawnKits(
    var playerKit: SpawnKit = SpawnKit.EMPTY,
    var teamKit: SpawnKit = SpawnKit.EMPTY,
)