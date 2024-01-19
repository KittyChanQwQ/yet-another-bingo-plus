package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.map.MapItemService
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.world.GameMode
import java.util.*

class ScoreboardHandler(
    private val server: MinecraftServer,
    private val scoreboardManager: IScoreboardManager,
) {

    val playersWithObjectives = mutableMapOf<UUID, MutableSet<String>>()

    fun playerHasObjective(playerEntity: PlayerEntity, objective: IObjectiveHandle) : Boolean {
        val set = playersWithObjectives.getOrPut(playerEntity.uuid) { mutableSetOf() }
        val hasObjective = set.contains(objective.name)
        set.add(objective.name)
        return hasObjective
    }

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

}