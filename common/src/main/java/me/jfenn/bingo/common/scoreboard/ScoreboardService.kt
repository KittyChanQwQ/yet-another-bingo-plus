package me.jfenn.bingo.common.scoreboard

import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.map.MapItemService
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.world.GameMode

class ScoreboardService(
    private val server: MinecraftServer,
    private val scoreboardManager: IScoreboardManager,
) {

    fun getScoreboardObjective(team: BingoTeam?) : IObjectiveHandle {
        val objectiveName = "bingo_sidebar_" + team?.name
        val objective = scoreboardManager.createDummyObjective(server, objectiveName, Text.literal("Minecraft BINGO"))
        return objective
    }

    fun shouldDisplayScoreboard(playerEntity: PlayerEntity) : Boolean {
        val isScoreboardEnabled = ConfigService.getPlayer(playerEntity.uuidAsString).scoreboard
        if (!isScoreboardEnabled) return false
        val player = server.playerManager.getPlayer(playerEntity.uuid) ?: return false

        val settings = ConfigService.getPlayer(playerEntity.uuidAsString)
        if (!settings.scoreboardAutoHide)
            return true

        // if the player is holding the map item, show the scoreboard; otherwise, hide it
        val isHoldingMapItem = player.interactionManager.gameMode != GameMode.SURVIVAL
                || MapItemService.isMapItem(playerEntity.mainHandStack)
                || MapItemService.isMapItem(playerEntity.offHandStack)

        return isHoldingMapItem
    }

    private val scoreboardViews = mutableMapOf<String, ScoreboardView>()

    fun setScoreboardContents(
        objective: IObjectiveHandle,
        contents: List<String>,
        players: List<ServerPlayerEntity>,
    ) {
        // update the server-side objective
        scoreboardManager.setScoreboardText(server, objective, contents)

        val view = scoreboardViews.getOrPut(objective.name) { ScoreboardView() }
        val changes = view.updateContents(contents)

        for (player in players) {
            if (!view.players.contains(player.uuid)) {
                // ensure that the player is only in one scoreboard view at a time
                for (otherView in scoreboardViews.values - view) {
                    if (otherView.players.contains(player.uuid)) {
                        otherView.players -= player.uuid
                        scoreboardManager.sendObjectiveDelete(player, objective)
                    }
                }

                // if the player is new, send scoreboard create packets
                scoreboardManager.sendObjectiveCreate(player, objective)
                scoreboardManager.sendScoreChanges(player, objective, view.createChanges(contents))
            } else {
                // otherwise, update scores that the player should already have
                scoreboardManager.sendScoreChanges(player, objective, changes)
            }
        }

        val playerUuids = players.map { it.uuid }.toSet()
        for (removedPlayerUuid in view.players - playerUuids) {
            // if a player is removed, send a delete packet
            val removedPlayer = server.playerManager.getPlayer(removedPlayerUuid) ?: continue
            scoreboardManager.sendObjectiveDelete(removedPlayer, objective)
        }

        view.players = playerUuids
    }

}