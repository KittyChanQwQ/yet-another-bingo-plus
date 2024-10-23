package me.jfenn.bingo.common.team

import me.jfenn.bingo.api.*
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting
import org.slf4j.Logger
import kotlin.math.ceil

class TeamService(
    private val log: Logger,
    private val server: MinecraftServer,
    private val events: ScopedEvents,
    private val state: BingoState,
    private val teamManager: ITeamManager,
    private val textProvider: TextProvider,
    private val playerManager: IPlayerManager,
    private val mapService: IMapService,
) {

    fun getPlayerTeam(player: IPlayerHandle) = getPlayerTeam(player.player)

    fun getPlayerTeam(player: ServerPlayerEntity): BingoTeam? {
        val team = teamManager.getPlayerTeam(player) ?: return null
        return state.teams[BingoTeamKey(team.name)]
    }

    fun isPlaying(player: IPlayerHandle) = isPlaying(player.player)

    fun isPlaying(player: ServerPlayerEntity): Boolean {
        return getPlayerTeam(player) != null
    }

    fun getTeamMap(team: BingoTeam): BingoMap {
        return team.map ?: run {
            val map = BingoMap(
                mapId = mapService.getNextMapId(),
            )

            log.info("Creating a map for team ${team.key.id} - mapId=${map.mapId}")
            team.map = map
            return@run map
        }
    }

    private fun createTeam(team: BingoTeam): ITeamHandle {
        return teamManager.createTeam(
            server,
            teamName = team.id,
            displayName = textProvider.string(team.name),
            color = team.formatting,
        )
    }

    fun joinTeam(player: IPlayerHandle, teamTemplate: BingoTeam) {
        if (getPlayerTeam(player)?.key == teamTemplate.key) return
        val team = state.registerTeam(teamTemplate)
        val playerProfile = BingoPlayerProfile.fromPlayer(player)

        // Remove player from their existing team
        getPlayerTeam(player)?.players?.remove(playerProfile)

        teamManager.setPlayerTeam(player.player, createTeam(team))
        team.players.add(playerProfile)
        player.sendMessage(
            textProvider.string(StringKey.LobbyTeamChanged, team.getName(textProvider))
                .formatted(Formatting.YELLOW)
        )

        events.onChangeTeam(player)
    }

    fun shuffleTeams(numTeams: Int) {
        val teams = BingoTeam.TEAM_BLOCKS.values.shuffled()

        val players = playerManager.getPlayers()
        val playersPerTeam = ceil(players.size / numTeams.coerceIn(1, teams.size).toDouble()).toInt()

        players.shuffled()
            .chunked(playersPerTeam)
            .forEachIndexed { i, teamPlayers ->
                val teamTemplate = teams[i % teams.size]
                for (teamPlayer in teamPlayers)
                    joinTeam(teamPlayer, teamTemplate)
            }
    }

}