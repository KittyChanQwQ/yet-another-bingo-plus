package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.GameState
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object BingoGame {

    fun start(ctx: BingoContext) {
        // if the game has already been started, ignore
        if (ctx.state.startedAt != null) return
        if (!ctx.config.isValid()) return

        // save the current config, if overwrite option is set
        if (ctx.config.overwrite) {
            ConfigService.writeConfig(ctx.config)
        }

        for (player in ctx.server.playerManager.playerList) {
            val team = BingoTeam.fromPlayer(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game is starting!")))
            Sounds.playGameStarted(player)
        }

        // spread player teams to spawnpoint
        SpawnService.createSpawnpoints(ctx)

        ChatMessages.broadcastStart(ctx)

        // change to the playing state
        ctx.state.changeState(ctx, GameState.PLAYING)
    }

    fun end(ctx: BingoContext, reason: String?) {
        ChatMessages.broadcastEnd(ctx, reason)
        ctx.state.changeState(ctx, GameState.POSTGAME)
    }

}