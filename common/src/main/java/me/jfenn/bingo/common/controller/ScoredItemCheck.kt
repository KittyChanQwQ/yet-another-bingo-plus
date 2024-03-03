package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.IOfflinePlayerAccessor
import me.jfenn.bingo.common.ChatMessages
import me.jfenn.bingo.common.GameService
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.card.BingoPlayerProfile
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.utils.allHeldStacks
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import java.util.*
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
    private val configService: ConfigService,
    private val offlinePlayerAccessor: IOfflinePlayerAccessor,
    private val advancementManager: IAdvancementManager,
) : BingoComponent() {

    private val offlinePlayerCache = mutableMapOf<BingoPlayerProfile, ServerPlayerEntity>()

    private fun getOfflinePlayer(profile: BingoPlayerProfile): ServerPlayerEntity {
        // If the player is online, return the current player entity
        val onlinePlayer = server.playerManager.getPlayer(UUID.fromString(profile.uuid))
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
        val player: ServerPlayerEntity
        measureTime {
            player = offlinePlayerAccessor.getOfflinePlayer(server, profile)
        }.also {
            log.info("Loading offline player data for ${profile.name} ($it)")
        }

        offlinePlayerCache[profile] = player
        return player
    }

    fun updateInventory(): Set<BingoTeam> {
        val updatedTeams = mutableSetOf<BingoTeam>()

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player) ?: continue

            // don't update teams that have already won
            if (team.isWinner()) continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get the item tile, if it is on the card
                val tile = state.card.itemIndex[stack.item] ?: continue

                // if the item hasn't been achieved and can be satisfied, mark it as collected
                if (tile.canSatisfy(stack)) {
                    tile.markSeen(team)
                } else {
                    continue
                }

                // if the gamemode is lockout, tiles cannot be collected by >1 team
                if (options.isLockoutMode && tile.hasAnyAchieved())
                    continue

                tile.setAchieved(team, true)
                    // if the tile has changed, mark updatedTeams
                    .also { isChanged -> if (isChanged) updatedTeams.add(team) }
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
                        player.inventory.containsAny { item.canSatisfy(it) }
                                || item.canSatisfy(player.currentScreenHandler.cursorStack)
                    }

                    // if noone on the team has the item anymore, mark it lost
                    if (!hasTeamItem) {
                        item.setAchieved(team, false)
                            // if the tile has changed, mark updatedTeams
                            .also { isChanged -> if (isChanged) updatedTeams.add(team) }
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

    fun updateAdvancements(): Set<BingoTeam> {
        val updatedTeams = mutableSetOf<BingoTeam>()

        for (entry in state.card.advancementEntries) {
            val advancement = advancementManager.getAdvancement(server, entry.identifierInstance)
                ?: continue

            for (team in state.getRegisteredTeams()) {
                // don't update teams that have already won
                if (team.isWinner()) continue

                val players = team.players.map { getOfflinePlayer(it) }

                val isAnyObtained = players.any {
                    advancementManager.isAnyObtained(it, advancement)
                }
                if (isAnyObtained) {
                    entry.markSeen(team)
                }

                val isDone = players.any {
                    advancementManager.isDone(it, advancement)
                }

                // if the advancement is finished, and the tile is not locked
                if (isDone && !(options.isLockoutMode && entry.hasAnyAchieved())) {
                    entry.setAchieved(team, true)
                        // if the tile has changed, mark updatedTeams
                        .also { isChanged -> if (isChanged) updatedTeams.add(team) }
                }

                // if noone on the team has the advancement anymore, and the game is in inventory mode
                // then mark it as lost...
                if (!isDone && options.isInventoryMode) {
                    entry.setAchieved(team, true)
                        // if the tile has changed, mark updatedTeams
                        .also { isChanged -> if (isChanged) updatedTeams.add(team) }
                }
            }
        }

        // update the stored score for each team
        for (team in updatedTeams) {
            state.updateScore(team)
        }

        return updatedTeams
    }

    fun updateScores() {
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
            if (scoreAfter.lines > scoreBefore.lines && options.showCompletedLines) {
                val message = Text.empty()
                    .append(team.text())
                    .append(
                        Text.literal(" has scored ${scoreAfter.lines} line${if (scoreAfter.lines > 1) "s" else ""}!")
                            .formatted(Formatting.YELLOW)
                    )

                server.playerManager.playerList
                    .filter { configService.getPlayer(it.uuidAsString).scoreMessages }
                    .forEach { it.sendMessage(message, false) }
            }

            if (scoreAfter.items > scoreBefore.items) {
                // play the doink sound
                Sounds.playTileCollected(server, team)
            }
        }

        // check if a team has won the game
        for (team in updatedTeams) {
            val isWinner = state.updateWinner(team)

            if (isWinner && options.isRankedMode) {
                // announce that another team has completed their card
                ChatMessages.broadcastTeamCompleted(server, team)
            }

            if (isWinner && !options.isRankedMode) {
                // end the game, since a team has won
                gameService.end( null)
                return
            }
        }

        // check if the game is in a stalemate
        if (ScoreService.isStalemate(state)) {
            gameService.end("Stalemate!")
            return
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = ScoreService.getLeading(state)
        if (prevLeadingTeam != leadingTeam && options.showLeadingTeam && state.getRegisteredTeams().size > 1) {
            // if the leading team should be shown, announce the new leader
            if (leadingTeam != null) {
                ChatMessages.broadcastScoreLeadingTeam(server, options, configService, leadingTeam)
            } else {
                updatedTeams.firstOrNull()?.let {
                    ChatMessages.broadcastScoreTiedTeam(server, options, configService, it)
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
    }

}