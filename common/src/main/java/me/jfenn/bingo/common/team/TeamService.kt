package me.jfenn.bingo.common.team

import me.jfenn.bingo.api.*
import me.jfenn.bingo.common.commands.CommandTreeHandler
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
    private val state: BingoState,
    private val teamManager: ITeamManager,
    private val commandTreeHandler: CommandTreeHandler,
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

    fun joinTeam(player: ServerPlayerEntity, teamTemplate: BingoTeam) {
        if (getPlayerTeam(player)?.key == teamTemplate.key) return
        val team = state.registerTeam(teamTemplate)
        val playerProfile = BingoPlayerProfile.fromPlayer(playerManager.forPlayer(player))

        // Remove player from their existing team
        val oldTeam = getPlayerTeam(player)
        if (oldTeam != null) {
            oldTeam.players.remove(playerProfile)

            if (!oldTeam.isWinner()) {
                // Remove any of the player's held/scored items from the card
                val card = state.getCard(oldTeam)
                if (card != null) {
                    for (objective in card.objectives.values) {
                        objective.removePlayer(playerProfile.uuid)
                    }
                }
            }
        }

        teamManager.setPlayerTeam(player, createTeam(team))
        team.players.add(playerProfile)
        player.sendMessage(
            textProvider.string(StringKey.LobbyTeamChanged, team.getName(textProvider))
                .formatted(Formatting.YELLOW)
        )

        onTeamChanged(player)
    }

    fun onTeamChanged(player: ServerPlayerEntity) {
        // Send a new command tree once player is on a team
        commandTreeHandler.sendCommandTree(player)
    }

    fun shuffleTeams(numTeams: Int) {
        val teams = BingoTeam.TEAM_BLOCKS.values.shuffled()

        val players = server.playerManager.playerList
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