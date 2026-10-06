package me.jfenn.bingo.client.world

import me.jfenn.bingo.common.BINGO_WORLD_PREFIX
import net.minecraft.world.level.storage.WorldData

object WorldMixinHelper {
    @JvmStatic
    fun isBingoWorld(worldData: WorldData) = worldData.levelName.startsWith(BINGO_WORLD_PREFIX)
}