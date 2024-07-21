package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameEndReason
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import kotlin.time.measureTime

/**
 * Searches for items in each player's inventory that can
 * satisfy a tile on the card, and updates the score if found.
 */
class ScoredItemCheck(
    events: ScopedEvents,
    private val log: Logger,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val gameService: GameService,
    private val teamService: TeamService,
    private val playerManager: IPlayerManager,
    private val advancementManager: IAdvancementManager,
    private val scoreUpdateService: ScoreUpdateService,
) : BingoComponent() {

    private val offlinePlayerCache = mutableMapOf<BingoPlayerProfile, IPlayerHandle>()

    private fun getOfflinePlayer(profile: BingoPlayerProfile): IPlayerHandle {
        // If the player is online, return the current player entity
        val onlinePlayer = playerManager.getPlayer(profile.uuid)
        if (onlinePlayer != null) {
            // and set their offline cache entry
            offlinePlayerCache[profile] = onlinePlayer
            return onlinePlayer
        }

        // If the player is in the offline cache, return it
        val cachedPlayer = offlinePlayerCache[profile]
        if (cachedPlayer != null) {
            return cachedPlayer
        }

        // Otherwise, load their player data from the save (this can take a few ms...)
        val player: IPlayerHandle
        measureTime {
            player = playerManager.getOfflinePlayer(profile)
        }.also {
            log.info("Loading offline player data for ${profile.name} ($it)")
        }

        offlinePlayerCache[profile] = player
        return player
    }

    private fun updateInventory(): Set<BingoTeam> {
        val updatedTeams = mutableSetOf<BingoTeam>()

        for (player in playerManager.getPlayers()) {
            val team = teamService.getPlayerTeam(player.player) ?: continue

            // don't update teams that have already won
            if (team.isWinner()) continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get the item tile, if it is on the card
                val tile = state.card.itemIndex[stack.identifier.toString()] ?: continue

                // if the item hasn't been achieved and can be satisfied, mark it as collected
                if (tile.canSatisfy(stack)) {
                    tile.markSeen(team)
                } else {
                    continue
                }

                // if the gamemode is lockout, tiles cannot be collected by >1 team
                if (options.isLockoutMode && tile.hasAnyAchieved())
                    continue

                val isChanged = tile.setAchieved(team, true)

                // if the tile has changed, mark updatedTeams
                if (isChanged) {
                    updatedTeams.add(team)

                    // record the player that captured the tile
                    val profile = BingoPlayerProfile.fromPlayer(player)
                    tile.players.add(profile)

                    scoreUpdateService.sendItemCaptured(
                        scoredPlayer = profile,
                        team = team,
                        item = stack,
                    )
                }
            }
        }

        // if isInventoryMode, we need to check that each team still has all their items
        // and mark them as lost if not
        if (options.isInventoryMode) {
            for (item in state.card.itemIndex.values) {
                for (teamKey in item.teams.toSet()) {
                    val team = state.teams[teamKey] ?: continue
                    // don't update teams that have already won
                    if (team.isWinner()) continue

                    val teamPlayers = team.players.map { getOfflinePlayer(it) }
                    val hasTeamItem = teamPlayers.any { player ->
                        player.allHeldStacks().any { item.canSatisfy(it) }
                    }

                    // if noone on the team has the item anymore, mark it lost
                    if (!hasTeamItem) {
                        val isChanged = item.setAchieved(team, false)
                        // if the tile has changed, mark updatedTeams
                        if (isChanged) {
                            updatedTeams.add(team)
                            // since the tile was lost, remove the team's players from the tile
                            item.players.removeAll(team.players)
                        }
                    }
                }
            }
        }

        // update the stored score for each team
        for (team in updatedTeams) {
            state.updateScore(team)
        }

        return updatedTeams
    }

    private fun updateAdvancements(): Set<BingoTeam> {
        val updatedTeams = mutableSetOf<BingoTeam>()

        for ((identifier, entry) in state.card.advancementIndex) {
            val advancement = advancementManager.getAdvancement(server, identifier)
                ?: continue

            for (team in state.getRegisteredTeams()) {
                // don't update teams that have already won
                if (team.isWinner()) continue

                val players = team.players.map { getOfflinePlayer(it) }

                val isAnyObtained = players.any {
                    advancementManager.isAnyObtained(it.player, advancement)
                }
                if (isAnyObtained) {
                    entry.markSeen(team)
                }

                val player = players.find {
                    advancementManager.isDone(it.player, advancement)
                }

                // if the advancement is finished, and the tile is not locked
                if (player != null && !(options.isLockoutMode && entry.hasAnyAchieved())) {
                    val isChanged = entry.setAchieved(team, true)

                    // if the tile has changed, mark updatedTeams
                    if (isChanged) {
                        updatedTeams.add(team)

                        // record the player that captured the tile
                        val profile = BingoPlayerProfile.fromPlayer(player)
                        entry.players.add(profile)

                        scoreUpdateService.sendItemCaptured(
                            scoredPlayer = profile,
                            team = team,
                            item = entry.visualItem(),
                        )
                    }
                }

                // if noone on the team has the advancement anymore, and the game is in inventory mode
                // then mark it as lost...
                if (player == null && options.isInventoryMode) {
                    val isChanged = entry.setAchieved(team, false)
                    // if the tile has changed, mark updatedTeams
                    if (isChanged) {
                        updatedTeams.add(team)
                        // since the tile was lost, remove the team's players from the tile
                        entry.players.removeAll(team.players)
                    }
                }
            }
        }

        // update the stored score for each team
        for (team in updatedTeams) {
            state.updateScore(team)
        }

        return updatedTeams
    }

    private fun updateScores() {
        val prevLeadingTeam = ScoreService.getLeading(state)
        val prevTeamScores = state.teams.mapValues { (_, team) -> team.score }

        val updatedTeams = updateInventory() + updateAdvancements()

        // if there are no updated teams, we can skip the remaining checks...
        if (updatedTeams.isEmpty()) return

        for (team in updatedTeams) {
            // update the score for each modified team
            val scoreBefore = prevTeamScores[team.key] ?: TeamScore.ZERO
            val scoreAfter = team.score

            // if the team has scored a line, post a message
            if (scoreAfter.lines > scoreBefore.lines) {
                scoreUpdateService.sendLineCaptured(team)
            }
        }

        // check if a team has won the game
        for (team in updatedTeams) {
            val isWinner = state.updateWinner(team)

            if (isWinner && options.isRankedMode) {
                // announce that another team has completed their card
                scoreUpdateService.sendCardCompleted(team)
            }

            if (isWinner && !options.isRankedMode) {
                // end the game, since a team has won
                gameService.end(GameEndReason.BINGO)
                return
            }
        }

        // check if the game is in a stalemate
        if (ScoreService.isLockout(state)) {
            gameService.end(GameEndReason.LOCKOUT)
            return
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = ScoreService.getLeading(state)
        if (prevLeadingTeam != leadingTeam && options.showLeadingTeam && state.getRegisteredTeams().size > 1) {
            // if the leading team should be shown, announce the new leader
            if (leadingTeam != null) {
                scoreUpdateService.sendTeamLeading(leadingTeam)
            } else {
                updatedTeams.firstOrNull()?.let {
                    scoreUpdateService.sendTeamTied(it)
                }
            }
        }
    }

    init {
        events.onGameTick {
            if (state.state == GameState.PLAYING) {
                updateScores()
            }
        }

        events.onEnter(GameState.POSTGAME) {
            // Mark all items as "seen" when game ends
            // (if hidden items is true, players should see all items after the game)
            for (team in state.teams.values) {
                for (item in state.card.itemIndex.values) {
                    item.markSeen(team)
                }
            }
        }
    }

}