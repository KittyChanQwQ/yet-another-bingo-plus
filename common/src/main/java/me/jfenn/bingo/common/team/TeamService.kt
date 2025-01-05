package me.jfenn.bingo.common.team

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting
import org.slf4j.Logger
import kotlin.math.ceil

class TeamService(
    private val log: Logger,
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
            teamName = team.id,
            displayName = team.getSimpleName(textProvider),
            color = team.textColor,
        )
    }

    fun joinTeam(player: IPlayerHandle, teamTemplate: BingoTeam) {
        if (getPlayerTeam(player)?.key == teamTemplate.key) return
        val team = state.registerTeam(teamTemplate)
        val playerProfile = player.profile

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

    private val extraTeamColors = arrayOf(
        Formatting.DARK_AQUA,
        Formatting.DARK_BLUE,
        Formatting.DARK_RED,
        Formatting.DARK_GREEN,
        Formatting.DARK_PURPLE,
    )

    fun shuffleTeams(numTeams: Int) {
        // Since teams are being shuffled, all existing teams should be cleared
        state.teams.clear()

        val teams = BingoTeam.TEAM_BLOCKS.values
            .shuffled()
            // Prefer shuffling onto teams that have the most players
            .sortedByDescending { state.teams[it.key]?.players?.size ?: 0 }
            .toMutableList()

        // If numTeams is more than the default team list, create temporary teams until full
        for (i in teams.size..numTeams) {
            val temporaryTeam = BingoTeam(
                id = "temp_bingo_$i",
                name = null,
                nameText = textProvider.serialized(
                    textProvider.string(StringKey.TeamName, "$i")
                ),
                symbol = null,
                textColor = extraTeamColors.find { color ->
                    teams.none { it.textColor == color }
                } ?: run {
                    (BingoTeam.TEAM_BLOCKS.values.map { it.textColor } + extraTeamColors).random()
                },
                isTemporary = true,
            )
            teams.add(temporaryTeam)
        }

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

    fun clearTeams() {
        for (team in BingoTeam.TEAM_BLOCKS.values + state.teams.values) {
            teamManager.deleteTeam(team.key.id)
        }
        state.teams.clear()
    }

}