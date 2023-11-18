package me.jfenn.bingo.integrations

import me.jfenn.bingo.*
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.ScoreService
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.utils.allHeldStacks
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Searches for items in each player's inventory that can
 * satisfy a tile on the card, and updates the score if found.
 */
object ScoredItemCheck {

    fun updateInventory(ctx: BingoContext) {
        val prevLeadingTeam = ScoreService.getLeading(ctx.state)
        val updatedTeams = mutableSetOf<BingoTeam>()

        for (player in ctx.server.playerManager.playerList) {
            val team = BingoTeam.fromPlayer(player) ?: continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get the item tile, if it is on the card
                val tile = ctx.state.card.itemIndex[stack.item] ?: continue

                // if the item hasn't been achieved and can be satisfied, mark it as collected
                if (!tile.canSatisfy(stack)) continue
                if (tile.hasAchieved(team)) continue

                markTileCollected(ctx, tile, team)
                    // if the tile has changed, mark updatedTeams
                    .also { isChanged -> if (isChanged) updatedTeams.add(team) }
            }
        }

        // if BingoMode.INVENTORY, we need to check that each team still has all their items
        // and mark them as lost if not
        if (ctx.config.gameMode == BingoMode.INVENTORY) {
            for (item in ctx.state.card.entries.filterIsInstance<BingoCardEntry.ItemEntry>()) {
                for (team in item.teams.toSet()) {
                    val teamInstance = team.team(ctx.server)
                    val hasTeamItem = ctx.server.playerManager.playerList
                        .filter { it.isTeamPlayer(teamInstance) }
                        .any { player -> player.inventory.containsAny { item.canSatisfy(it) }
                                || item.canSatisfy(player.currentScreenHandler.cursorStack) }

                    // if noone on the team has the item anymore, mark it lost
                    if (!hasTeamItem) {
                        markTileLost(item, team)
                            // if the tile has changed, mark updatedTeams
                            .also { isChanged -> if (isChanged) updatedTeams.add(team) }
                    }
                }
            }
        }

        // if there are no updated teams, we can skip the remaining checks...
        if (updatedTeams.isEmpty()) return

        // update the team map(s)
        if (ctx.config.gameMode == BingoMode.LOCKOUT || ctx.config.drawOtherTeamCompletions) {
            // if the mode is LOCKOUT, all team cards need to be updated (to redraw locked items)
            for (team in BingoTeam.entries)
                MapRenderService.update(ctx, team)
        } else {
            // otherwise, only redraw maps for the updated teams
            for (team in updatedTeams)
                MapRenderService.update(ctx, team)
        }

        // check if a team has won the game
        ScoreService.getWinner(ctx.state)?.let {
            BingoGame.end(ctx, null)
            return
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

    /**
     * @return if the given tile has been changed
     */
    private fun markTileCollected(ctx: BingoContext, tile: BingoCardEntry, team: BingoTeam): Boolean {
        // if the gamemode is lockout, tiles cannot be collected by >1 team
        if (ctx.config.gameMode == BingoMode.LOCKOUT && tile.hasAnyAchieved())
            return false
        // if the team has already achieved the tile, there is nothing to change
        if (tile.hasAchieved(team))
            return false

        // unlock the collected item
        val linesBefore = ctx.state.getTeamScore(team).lines
        tile.setAchieved(team, true)
        val linesAfter = ctx.state.updateScore(team).lines

        if (linesAfter > linesBefore && ctx.config.showCompletedLines) {
            val message = Text.empty()
                .append(team.text(ctx.server))
                .append(
                    Text.literal(" has scored $linesAfter line${if (linesAfter > 1) "s" else ""}!")
                        .formatted(Formatting.YELLOW)
                )

            ctx.server.playerManager.playerList
                .filter { ConfigService.getPlayer(it.uuidAsString).scoreMessages }
                .forEach { it.sendMessage(message, false) }
        }

        // play the doink sound
        Sounds.playTileCollected(ctx, team)
        return true
    }

    /**
     * This is only used in BingoMode.INVENTORY to un-mark a tile if
     * the item is no longer in a player's inventory
     *
     * @return if the given tile has been changed
     */
    private fun markTileLost(tile: BingoCardEntry, team: BingoTeam): Boolean {
        // if the team doesn't have the tile, there is nothing to change
        if (!tile.hasAchieved(team)) return false

        tile.setAchieved(team, false)
        return true
    }

    fun init() {
        Bingo.onGameTick {
            if (state.state == GameState.PLAYING) {
                updateInventory(this)
            }
        }
    }

}