package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.BINGO
import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.datapack.LobbyConfigService
import me.jfenn.bingo.common.menu.poi.MenuPoiState
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.platform.IMinecraftServer
import me.jfenn.bingo.platform.IServerWorldFactory
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

internal class LobbyWorldService(
    private val log: Logger,
    private val serverWorldFactory: IServerWorldFactory,
    private val lobbyConfigService: LobbyConfigService,
    private val server: IMinecraftServer,
    private val menuPoiState: MenuPoiState,
) {
    companion object {
        private const val WORLD_NAME = "Bingo Lobby"
        private const val LOBBY_DIMENSION_PREFIX = "dimensions/bingo/lobby/"
        private const val LOBBY_POI_FILE = "data/$MOD_ID_BINGO/lobby_poi.json.gz"
    }

    fun prepareLobbyFiles() = log.measureTime("[LobbyWorldService] Preparing the BINGO lobby...") {
        val storageDir = serverWorldFactory.getLevelSaveDir(LOBBY_WORLD_ID)
            ?: error("Unable to find world storage dir for $LOBBY_WORLD_ID")

        lobbyConfigService.openLobbyZip().use { zipStream ->
            while (true) {
                val entry = zipStream.nextEntry ?: break
                if (entry.isDirectory) continue

                val entryName = (
                        entry.name.takeIf { it.startsWith(LOBBY_DIMENSION_PREFIX) }
                            ?: entry.name.substringAfter('/').takeIf { it.startsWith(LOBBY_DIMENSION_PREFIX) }
                        )
                    ?.substring(LOBBY_DIMENSION_PREFIX.length)

                if (entryName != null) {
                    val outPath = storageDir.resolve(entryName)
                    if (!outPath.startsWith(storageDir)) {
                        log.error("[LobbyWorldService] Tried to create a file outside of the world location: $outPath")
                        continue
                    }

                    Files.createDirectories(outPath.parent)

                    log.debug("Copying file -> {}", outPath)
                    try {
                        Files.copy(zipStream, outPath, StandardCopyOption.REPLACE_EXISTING)
                    } catch (e: IOException) {
                        log.error("[LobbyWorldService] Error copying lobby file $entryName", e)
                    }
                }

                zipStream.closeEntry()
            }
        }

        menuPoiState.read(storageDir.resolve(LOBBY_POI_FILE))
    }

    fun writeLobbyFiles() {
        server.saveAll()

        val baseDir = serverWorldFactory.baseDir
        val storageDir = serverWorldFactory.getLevelSaveDir(LOBBY_WORLD_ID)
            ?: error("Unable to find world storage dir for $LOBBY_WORLD_ID")

        menuPoiState.write(storageDir.resolve(LOBBY_POI_FILE))

        lobbyConfigService.writeLobbyZip().use { zipOutput ->
            // copy every file in dimensions/bingo/lobby/...
            for (file in storageDir.walk()) {
                val filePath = Path(WORLD_NAME).resolve(file.relativeTo(baseDir))
                zipOutput.putNextEntry(ZipEntry(filePath.toString()))
                file.inputStream().use {
                    IOUtils.copy(it, zipOutput)
                }
                zipOutput.flush()
                zipOutput.closeEntry()
            }

            // copy every file from the lobby zip
            baseDir.resolve("datapacks/$BINGO.zip")
                .inputStream()
                .let { ZipInputStream(it) }
                .use { datapackStream ->
                    while (true) {
                        val entry = datapackStream.nextEntry ?: break
                        if (entry.isDirectory) continue

                        val outputName = Path(WORLD_NAME).resolve("datapacks/bingo").resolve(entry.name.removePrefix("/"))

                        zipOutput.putNextEntry(ZipEntry(outputName.toString()))
                        IOUtils.copy(datapackStream, zipOutput)
                        zipOutput.flush()
                        zipOutput.closeEntry()

                        datapackStream.closeEntry()
                    }
                }

            // copy level.dat
            zipOutput.putNextEntry(ZipEntry(Path(WORLD_NAME, "level.dat").toString()))
            baseDir.resolve("level.dat").inputStream().use {
                IOUtils.copy(it, zipOutput)
            }
            zipOutput.flush()
            zipOutput.closeEntry()
        }
    }
}