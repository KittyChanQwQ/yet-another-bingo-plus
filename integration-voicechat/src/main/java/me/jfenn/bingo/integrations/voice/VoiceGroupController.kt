package me.jfenn.bingo.integrations.voice

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import me.jfenn.bingo.api.BingoApi
import me.jfenn.bingo.api.BingoEvents
import me.jfenn.bingo.api.data.BingoGameStatus
import me.jfenn.bingo.api.data.IBingoTeam
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.IServerEvents
import me.jfenn.bingo.platform.config.IConfigManager
import java.io.Closeable
import kotlin.reflect.KProperty0

class VoiceGroupController(
    configManager: IConfigManager,
    private val playerManager: IPlayerManager,
    voiceIntegrationProp: KProperty0<VoiceIntegration?>,
    serverEvents: IServerEvents,
) : Closeable {

    private val voiceIntegration by voiceIntegrationProp

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
        }

        private const val CONFIG_PATH = "yet-another-minecraft-bingo/voicechat.json"
    }

    @OptIn(ExperimentalSerializationApi::class)
    val config: VoiceConfig = try {
        configManager.inputStream(CONFIG_PATH).use {
            json.decodeFromStream(it)
        }
    } catch (e: Throwable) {
        val defaultConfig = VoiceConfig()
        configManager.outputStream(CONFIG_PATH).use {
            json.encodeToStream(defaultConfig, it)
        }
        defaultConfig
    }

    private var usingCombinedGroup: Boolean? = null
    private fun isUsingCombinedGroup(): Boolean {
        usingCombinedGroup?.let { return it }

        val isSingleplayerTeams = BingoApi.teams?.all { it.players.size <= 1 } == true
        usingCombinedGroup = isSingleplayerTeams && config.useCombinedGroupForSingleplayerTeams
        return isSingleplayerTeams
    }

    private var combinedGroup: IGroupHandle? = null
        get() = field ?: run {
            val settings = config.combinedGroup ?: return@run null
            field = voiceIntegration?.createGroup(settings)
            field
        }


    private var spectatorsGroup: IGroupHandle? = null
        get() = field ?: run {
            val settings = config.spectatorGroup ?: return@run null
            field = voiceIntegration?.createGroup(settings)
            field
        }

    private val teamGroups = mutableMapOf<String, IGroupHandle?>()
    private fun createTeamGroup(team: IBingoTeam): IGroupHandle? {
        return teamGroups.getOrPut(team.id) {
            val settings = config.teamGroups ?: return@getOrPut null
            val teamName = team.id.removePrefix("bingo_")
            val name = when {
                settings.name.contains("%s") -> settings.name.replace("%s", teamName)
                else -> "${settings.name}: $teamName"
            }
            voiceIntegration?.createGroup(settings.copy(name = name))
        }
    }

    private fun getGroup(team: IBingoTeam?): IGroupHandle? {
        val status = BingoApi.game?.status
        return when {
            status == BingoGameStatus.PREGAME -> null
            status == BingoGameStatus.POSTGAME -> combinedGroup
            config.useCombinedGroupAlways -> combinedGroup
            team == null -> spectatorsGroup
            isUsingCombinedGroup() -> combinedGroup
            else -> createTeamGroup(team)
        }
    }

    private fun updateGroups(players: List<IPlayerHandle>) {
        val teams = BingoApi.teams ?: return

        for (player in players) {
            val team = teams.find { it.players.contains(player.uuid) }
            getGroup(team)?.addPlayer(player)
        }
    }

    private fun onStateChange() {
        // reset usingCombinedGroup on state change or between rounds
        usingCombinedGroup = null

        val playersToUpdate = when {
            BingoApi.config?.isLobbyMode == true -> playerManager.getPlayers()
            else -> playerManager.getPlayers().filter { player ->
                BingoApi.teams?.any { it.players.contains(player.uuid) } == true
            }
        }
        updateGroups(playersToUpdate)
    }

    private val callbacks = mutableListOf<Closeable>()

    init {
        BingoEvents.GAME_STARTING.register {
            onStateChange()
        }.let { callbacks.add(it) }

        BingoEvents.GAME_ENDED.register {
            onStateChange()
            // Clean up the team groups, which should now be unused
            teamGroups.values.forEach { it?.close() }
            teamGroups.clear()
        }.let { callbacks.add(it) }

        BingoEvents.GAME_RESET.register {
            // Remove players from all groups
            // (as voicechat never deletes groups that have players in them)
            playerManager.getPlayers().forEach { player ->
                combinedGroup?.removePlayer(player)
                spectatorsGroup?.removePlayer(player)
                teamGroups.values.forEach { it?.removePlayer(player) }
            }

            usingCombinedGroup = null
            combinedGroup?.close()
            combinedGroup = null
            spectatorsGroup?.close()
            spectatorsGroup = null
            teamGroups.values.forEach { it?.close() }
            teamGroups.clear()
        }.let { callbacks.add(it) }

        BingoEvents.TEAM_CHANGED.register { event ->
            playerManager.getPlayer(event.player)
                ?.let { updateGroups(listOf(it)) }
        }.let { callbacks.add(it) }

        serverEvents.onPlayerJoin {
            updateGroups(listOf(it.player))
        }.let { callbacks.add(it) }
    }

    override fun close() {
        callbacks.forEach { it.close() }
    }
}
