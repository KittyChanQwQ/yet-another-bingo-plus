package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.common.BingoKoin
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.integrations.ScoreboardHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer

object ScoreboardMixinHandler {

    fun getScoreboardObjective(server: MinecraftServer, team: BingoTeam?): IObjectiveHandle? {
        val scope = BingoKoin.getScope(server) ?: return null
        val scoreboardHandler = scope.get<ScoreboardHandler>()
        return scoreboardHandler.getScoreboardObjective(team)
    }

    fun shouldDisplayScoreboard(server: MinecraftServer, player: PlayerEntity): Boolean {
        val scope = BingoKoin.getScope(server) ?: return false
        val scoreboardHandler = scope.get<ScoreboardHandler>()
        return scoreboardHandler.shouldDisplayScoreboard(player)
    }

}