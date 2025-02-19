package me.jfenn.bingo.common.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.BingoTeamPreset
import me.jfenn.bingo.common.utils.json
import net.minecraft.resource.ResourceManager
import org.slf4j.Logger

class TeamPresetLoader(
    private val log: Logger,
) {

    fun loadTeamPresets(
        manager: ResourceManager,
    ): Map<BingoTeamKey, BingoTeamPreset> {
        log.info("[TagLoader] Reloading BINGO objective tags...")

        val newTeamPresets = mutableMapOf<BingoTeamKey, BingoTeamPreset>()

        val lists = manager.findResources(TEAM_PRESETS_PATH) { it.path.endsWith(FILE_SUFFIX) }
        for ((id, file) in lists) {
            val name = id.path.substringAfterLast('/').removeSuffix(FILE_SUFFIX)
            val config = try {
                file.inputStream.use {
                    @OptIn(ExperimentalSerializationApi::class)
                    json.decodeFromStream<BingoTeamPreset>(it)
                }
            } catch (e: Throwable) {
                log.error("[TagLoader] Error reading tag file $id: ${e.message}")
                continue
            }

            newTeamPresets[BingoTeamKey(name)] = config
        }

        return newTeamPresets
    }

    companion object {
        private const val FILE_SUFFIX = ".json"
        private const val TEAM_PRESETS_PATH = "$MOD_ID_BINGO/teams"
    }
}