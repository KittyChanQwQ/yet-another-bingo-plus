package me.jfenn.bingo

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

/**
 * This deletes all world files that should reset when the
 * server restarts. (leaving behind configs, datapacks, & player statistics)
 */
object WorldDeleter {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val FILES_TO_RM = listOf(
        "world/advancements",
        "world/data",
        "world/DIM1",
        "world/DIM-1",
        "world/dimensions",
        "world/entities",
        "world/playerdata",
        "world/poi",
        "world/region",
        "world/stats",
        "world/level.dat",
        "world/level.dat_old",
    )

    fun invoke() {
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            log.warn("Skipping erroneous WorldDeleter call on EnvType.CLIENT")
            return
        }

        val gameDir = FabricLoader.getInstance().gameDir
        log.info("Deleting world files in dir: ${gameDir.toAbsolutePath()}")

        measureTime {
            for (fileName in FILES_TO_RM) {
                val file = gameDir.resolve(fileName).toFile()
                file.deleteRecursively()
            }
        }.also {
            log.info("Done ($it)")
        }
    }
}
