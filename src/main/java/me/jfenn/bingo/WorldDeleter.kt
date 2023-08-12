package me.jfenn.bingo

import net.fabricmc.loader.api.FabricLoader

/**
 * This deletes all world files that should reset when the
 * server restarts. (leaving behind configs, datapacks, & player statistics)
 */
object WorldDeleter {

    val FILES_TO_RM = listOf(
        "world/advancements",
        "world/dimensions",
        "world/data",
        "world/DIM1",
        "world/DIM-1",
        "world/dimensions",
        "world/entities",
        "world/playerdata",
        "world/poi",
        "world/region",
        "world/level.dat"
    )

    fun invoke() {
        val gameDir = FabricLoader.getInstance().gameDir

        for (fileName in FILES_TO_RM) {
            val file = gameDir.resolve(fileName).toFile()
            file.deleteRecursively()
        }
    }
}
