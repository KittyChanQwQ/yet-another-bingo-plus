package me.jfenn.bingo

import com.mojang.brigadier.builder.LiteralArgumentBuilder.*
import me.jfenn.bingo.event.PlayerEvents
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.map.BingoMapService
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.command.SpreadPlayersCommand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.Heightmap

const val MOD_ID = "bingo"

lateinit var minecraftServer: MinecraftServer

object Bingo : ModInitializer {

    override fun onInitialize() {
        println("$MOD_ID mod initialized (main)")

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            minecraftServer = server
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            if (BingoGame.state == GameState.PLAYING) {
                for (player in server.playerManager.playerList) {
                    val team = BingoTeam.fromPlayer(player) ?: continue

                    // check inventory for changes
                    for (item in BingoGame.card.entries) if (!item.hasAchieved(team)) {
                        val hasItem = player.inventory.containsAny { it.item == item.item() }
                        if (hasItem) BingoGame.markTileCollected(item, team)
                    }
                }
            }

            // TODO: move this elsewhere?
            for (player in server.playerManager.playerList) {
                BingoGame.giveMapCardItem(player)
            }
        }

        PlayerEvents.onPlayerJoin {
            BingoGame.updateGameMode(it)
            BingoGame.giveMapCardItem(it)
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { player, _, _ ->
            // give player a new map, if they don't have one?
            BingoGame.giveMapCardItem(player)
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            val team = BingoTeam.fromPlayer(player) ?: return@register TypedActionResult.pass(null)

            val itemStack = player.getStackInHand(hand)

            // if the item is the player's bingo map, open the item details chest
            if (itemStack.item == Items.FILLED_MAP && itemStack.nbt?.getInt("map") == team.mapId) {
                CardService.showInventory(player, BingoGame.card)
                return@register TypedActionResult.consume(null)
            }

            TypedActionResult.pass(null)
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            dispatcher.register(literal<ServerCommandSource?>("start").executes { ctx ->
                // TODO: sort teams evenly
                for (player in ctx.source.server.playerManager.playerList) {
                    // TODO: temporary, remove this
                    if (BingoTeam.fromPlayer(player) == null)
                        BingoTeam.BLUE.addPlayer(player)
                }

                // generate a new card for the round
                BingoGame.start()

                1
            })
        }
    }

}
