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

    fun rollCard(ctx: BingoContext) {
        ctx.state.card = CardService.generate(ctx.config)

        // update the card map images
        for (team in BingoTeam.entries) {
            MapRenderService.update(ctx, team)
        }
    }

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

    /**
     * Ensure that the player has their team's bingo map card.
     * Has no effect if the player already has a card
     */
    fun giveMapCardItem(ctx: BingoContext, player: ServerPlayerEntity) {
        val team = BingoTeam.fromPlayer(player) ?: return
        val hasMap = player.allHeldStacks().any { MapItemService.isMapTeamItem(ctx, it, team) }

        if (!hasMap) {
            // if the player has another team's map item, remove it!
            for ((slot, item) in player.allInventorySlots()) {
                if (MapItemService.isMapItem(item))
                    player.inventory.removeStack(slot)
            }

            // give the player a new map item
            val mapItem = MapItemService.createMapItem(ctx, team)
            player.giveItemStack(mapItem)
        }
    }

    /**
     * Ensure that the player has every team's bingo map card
     * in their hotbar slots. Indiscriminately replaces any existing
     * hotbar items.
     */
    fun giveSpectatorMapItems(ctx: BingoContext, player: ServerPlayerEntity) {
        BingoTeam.entries
            .filter { it.team(player.server).playerList.isNotEmpty() }
            .map { MapItemService.createMapItem(ctx, it) }
            .forEachIndexed { i, stack ->
                val mapId = FilledMapItem.getMapId(stack)
                val hasMap = player.allHeldStacks().any { FilledMapItem.getMapId(it) == mapId }
                if (!hasMap) player.getStackReference(i).set(stack)
            }
    }

    /**
     * Equip the player with elytra and firework rockets.
     * Only runs if config.isElytra is true
     */
    fun giveElytra(player: ServerPlayerEntity) {
        val hasElytra = player.allHeldStacks().any { it.item == Items.ELYTRA && it.nbt?.getBoolean(NBT_BINGO_VANISH) == true }
        if (!hasElytra) {
            // insert elytra in armor slot
            player.giveOrEquipStack(ItemStack(Items.ELYTRA, 1).also {
                it.getOrCreateNbt().apply {
                    // unbreakable (should not take damage)
                    putBoolean("Unbreakable", true)
                    putBoolean(NBT_BINGO_IGNORE, true)
                    putBoolean(NBT_BINGO_VANISH, true)
                }
            })
        }

        // also give rocket
        val rocketStack = player.allHeldStacks().find { it.item == Items.FIREWORK_ROCKET }

        if (rocketStack != null && rocketStack.count < 2) {
            rocketStack.count = 2
        }

        if (rocketStack == null) {
            player.giveItemStack(ItemStack(Items.FIREWORK_ROCKET, 2).also {
                it.getOrCreateNbt().apply {
                    putBoolean(NBT_BINGO_IGNORE, true)
                    putBoolean(NBT_BINGO_VANISH, true)
                    put("Fireworks", NbtCompound())
                }
            })
        }
    }

}