package me.jfenn.bingo.integrations.voice

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.formatTitle

class VoiceGroupController(
    events: ScopedEvents,
    private val state: BingoState,
    private val config: BingoConfig,
    private val teamService: TeamService,
    private val playerManager: IPlayerManager,
    private val voiceIntegration: VoiceIntegration,
) {
    private var usingCombinedGroup: Boolean? = null
    private fun isUsingCombinedGroup(): Boolean {
        usingCombinedGroup?.let { return it }

        val isSingleplayerTeams = state.getRegisteredTeams().all { it.players.size <= 1 }
        usingCombinedGroup = isSingleplayerTeams && config.integrations.voice.useCombinedGroupForSingleplayerTeams
        return isSingleplayerTeams
    }

    private val combinedGroup: IGroupHandle? by lazy {
        val settings = config.integrations.voice.combinedGroup ?: return@lazy null
        voiceIntegration.createGroup(settings)
    }

    private val spectatorsGroup: IGroupHandle? by lazy {
        val settings = config.integrations.voice.spectatorGroup ?: return@lazy null
        voiceIntegration.createGroup(settings)
    }

    private val teamGroups = mutableMapOf<BingoTeamKey, IGroupHandle?>()
    private fun createTeamGroup(team: BingoTeam): IGroupHandle? {
        return teamGroups.getOrPut(team.key) {
            val settings = config.integrations.voice.teamGroups ?: return@getOrPut null
            val teamName = team.key.label.formatTitle()
            val name = when {
                settings.name.contains("%s") -> settings.name.replace("%s", teamName)
                else -> "${settings.name}: $teamName"
            }
            voiceIntegration.createGroup(settings.copy(name = name))
        }
    }

    private fun getGroup(team: BingoTeam?): IGroupHandle? {
        return when {
            state.state == GameState.PREGAME -> null
            state.state == GameState.POSTGAME -> combinedGroup
            config.integrations.voice.useCombinedGroupAlways -> combinedGroup
            team == null -> spectatorsGroup
            isUsingCombinedGroup() -> combinedGroup
            else -> createTeamGroup(team)
        }
    }

    private fun updateGroups(players: List<IPlayerHandle>) {
        for (player in players) {
            val team = teamService.getPlayerTeam(player)
            getGroup(team)?.addPlayer(player)
        }
    }

    init {
        events.onStateChange {
            val playersToUpdate = when {
                state.isLobbyMode -> playerManager.getPlayers()
                else -> playerManager.getPlayers().filter { teamService.isPlaying(it) }
            }
            updateGroups(playersToUpdate)
        }

        events.onEnter(GameState.POSTGAME) {
            // Clean up the team groups, which should now be unused
            teamGroups.values.forEach { it?.close() }
            teamGroups.clear()
        }

        events.onChangeTeam {
            updateGroups(listOf(it))
        }

        events.onPlayerJoin {
            updateGroups(listOf(it))
        }
    }
}
