package me.jfenn.bingo.common.integrations

import com.mojang.authlib.GameProfile
import me.jfenn.bingo.api.IOfflinePlayerAccessor
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.BingoGame
import me.jfenn.bingo.common.ChatMessages
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.BingoPlayerProfile
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.ScoreService
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.utils.allHeldStacks
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

/**
 * Searches for items in each player's inventory that can
 * satisfy a tile on the card, and updates the score if found.
 */
object ScoredItemCheck : KoinComponent {

    private val offlinePlayerAccessor by inject<IOfflinePlayerAccessor>()
    private val offlinePlayerCache = mutableMapOf<BingoPlayerProfile, ServerPlayerEntity>()

    private fun getOfflinePlayer(ctx: BingoContext, profile: BingoPlayerProfile): ServerPlayerEntity {
        // If the player is online, return the current player entity
        val onlinePlayer = ctx.server.playerManager.getPlayer(UUID.fromString(profile.uuid))
        if (onlinePlayer != null) {
            // and remove any offline cache entry, if they have one
            offlinePlayerCache.remove(profile)
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
            player = offlinePlayerAccessor.getOfflinePlayer(ctx.server, profile)
        }.also {
            Bingo.log.info("Loading offline player data for ${profile.name} ($it)")
        }

        offlinePlayerCache[profile] = player
        return player
    }

    fun updateInventory(ctx: BingoContext): Set<BingoTeam> {
        val updatedTeams = mutableSetOf<BingoTeam>()

        for (player in ctx.server.playerManager.playerList) {
            val team = BingoTeam.fromPlayer(player) ?: continue

            // don't update teams that have already won
            if (team.isWinner()) continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get the item tile, if it is on the card
                val tile = ctx.state.card.itemIndex[stack.item] ?: continue

                // if the item hasn't been achieved and can be satisfied, mark it as collected
                if (tile.canSatisfy(stack)) {
                    tile.markSeen(team)
                } else {
                    continue
                }

                // if the gamemode is lockout, tiles cannot be collected by >1 team
                if (ctx.config.isLockoutMode && tile.hasAnyAchieved())
                    continue

                tile.setAchieved(team, true)
                    // if the tile has changed, mark updatedTeams
                    .also { isChanged -> if (isChanged) updatedTeams.add(team) }
            }
        }

        // if isInventoryMode, we need to check that each team still has all their items
        // and mark them as lost if not
        if (ctx.config.isInventoryMode) {
            for (item in ctx.state.card.entries.filterIsInstance<BingoCardEntry.ItemEntry>()) {
                for (teamKey in item.teams.toSet()) {
                    val team = ctx.state.teams[teamKey] ?: continue
                    // don't update teams that have already won
                    if (team.isWinner()) continue

                    val teamPlayers = team.players.map { getOfflinePlayer(ctx, it) }
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
            ctx.state.updateScore(team)
        }

        return updatedTeams
    }

    fun updateScores(ctx: BingoContext) {
        val prevLeadingTeam = ScoreService.getLeading(ctx.state)
        val prevTeamScores = ctx.state.teams.mapValues { (_, team) -> team.score }

        val updatedTeams = updateInventory(ctx)

        // if there are no updated teams, we can skip the remaining checks...
        if (updatedTeams.isEmpty()) return

        for (team in updatedTeams) {
            // update the score for each modified team
            val scoreBefore = prevTeamScores[team.key] ?: TeamScore.ZERO
            val scoreAfter = team.score

            // if the team has scored a line, post a message
            if (scoreAfter.lines > scoreBefore.lines && ctx.config.showCompletedLines) {
                val message = Text.empty()
                    .append(team.text(ctx.server))
                    .append(
                        Text.literal(" has scored ${scoreAfter.lines} line${if (scoreAfter.lines > 1) "s" else ""}!")
                            .formatted(Formatting.YELLOW)
                    )

                ctx.server.playerManager.playerList
                    .filter { ConfigService.getPlayer(it.uuidAsString).scoreMessages }
                    .forEach { it.sendMessage(message, false) }
            }

            if (scoreAfter.items > scoreBefore.items) {
                // play the doink sound
                Sounds.playTileCollected(ctx, team)
            }
        }

        // check if a team has won the game
        for (team in updatedTeams) {
            val isWinner = ctx.state.updateWinner(team)

            if (isWinner && ctx.config.isRankedMode) {
                // announce that another team has completed their card
                ChatMessages.broadcastTeamCompleted(ctx, team)
            }

            if (isWinner && !ctx.config.isRankedMode) {
                // end the game, since a team has won
                BingoGame.end(ctx, null)
                return
            }
        }

        // check if the game is in a stalemate
        if (ScoreService.isStalemate(ctx.state)) {
            BingoGame.end(ctx, "Stalemate!")
            return
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = ScoreService.getLeading(ctx.state)
        if (prevLeadingTeam != leadingTeam && ctx.config.showLeadingTeam) {
            // if the leading team should be shown, announce the new leader
            if (leadingTeam != null) {
                ChatMessages.broadcastScoreLeadingTeam(ctx, leadingTeam)
            } else {
                updatedTeams.firstOrNull()?.let {
                    ChatMessages.broadcastScoreTiedTeam(ctx, it)
                }
            }
        }
    }

    fun init() {
        Bingo.onGameTick {
            if (state.state == GameState.PLAYING) {
                updateScores(this)
            }
        }
    }

}