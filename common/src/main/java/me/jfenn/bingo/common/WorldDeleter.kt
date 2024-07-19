package me.jfenn.bingo.common

import me.jfenn.bingo.common.datapack.ServerProps
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
        "advancements",
        "data",
        "DIM1",
        "DIM-1",
        "dimensions",
        "entities",
        "playerdata",
        "poi",
        "region",
        "stats",
        "level.dat",
        "level.dat_old",
    )

    fun invoke() {
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            log.warn("Skipping erroneous WorldDeleter call on EnvType.CLIENT")
            return
        }

        val gameDir = FabricLoader.getInstance().gameDir.resolve(ServerProps.levelName)
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
