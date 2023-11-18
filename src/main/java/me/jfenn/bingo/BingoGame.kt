package me.jfenn.bingo

import me.jfenn.bingo.card.*
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.config.config
import me.jfenn.bingo.map.MapItemService
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.menu.MenuService
import me.jfenn.bingo.spawn.SpawnService
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.utils.allHeldStacks
import me.jfenn.bingo.utils.allInventorySlots
import me.jfenn.bingo.utils.formatString
import me.jfenn.bingo.utils.giveOrEquipStack
import net.minecraft.entity.boss.BossBar
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.message.SentMessage.Chat
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules
import java.time.Duration
import java.time.Instant

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

        // change to the playing state
        ctx.state.changeState(ctx, GameState.PLAYING)
    }

    fun end(ctx: BingoContext, reason: String?) {
        ChatMessages.broadcastEnd(ctx, reason)
        ctx.state.changeState(ctx, GameState.POSTGAME)
    }

}