package me.jfenn.bingo.common

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.datapack.ServerProps
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.time.measureTime

/**
 * This deletes all world files that should reset when the
 * server restarts. (leaving behind configs, datapacks, & player statistics)
 */
object WorldDeleter {

    private val log = LoggerFactory.getLogger("bingo")

    private val config = Config.read(BingoConfig::class, "$MOD_ID_BINGO/config.json")

    private fun deleteMatching(cwd: Path, path: String) {
        val part = path.substringBefore("/")
        val after = if (path.length > part.length + 1) {
            path.substring(part.length + 1)
        } else ""

        val matches = cwd.listDirectoryEntries(part)
        for (match in matches) {
            if (after.isEmpty()) {
                log.debug("Deleting file {}", match)
                match.toFile().deleteRecursively()
            } else {
                deleteMatching(match, after)
            }
        }
    }

    fun invoke() {
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            log.warn("Skipping erroneous WorldDeleter call on EnvType.CLIENT")
            return
        }

        val gameDir = FabricLoader.getInstance().gameDir.resolve(ServerProps.levelName)
        log.info("Deleting world files in dir: ${gameDir.toAbsolutePath()}")

        measureTime {
            for (fileName in config.server.filesToReset) {
                deleteMatching(gameDir, fileName)
            }
        }.also {
            log.info("Done ($it)")
        }
    }
}
