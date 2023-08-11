package me.jfenn.bingo

import com.mojang.brigadier.builder.LiteralArgumentBuilder.*
import me.jfenn.bingo.event.PlayerEvents
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.menu.PlaceStructure
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.Unit
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d

const val MOD_ID = "bingo"

lateinit var minecraftServer: MinecraftServer

object Bingo : ModInitializer {

    val lobbyWorld by lazy {
        minecraftServer.worlds
            .find { it.registryKey.value == Identifier.of("bingo", "lobby") }!!
            .also {
                // always load the spawn chunks
                it.chunkManager.addTicket(ChunkTicketType.START, ChunkPos(0, 0), 11, Unit.INSTANCE)
            }
    }

    override fun onInitialize() {
        println("$MOD_ID mod initialized (main)")

        // TODO:
        // - spawn area worldborder
        // - spawn area saturation
        // - spawn team picker, fix hitboxes, give interaction feedback
        // - spawn team shuffle tool
        // ? fix transparent items (glass) not rendering at all
        // - post-game, give all team cards to players
        // - add /coords command
        // - card /reroll command
        // - send to team chat by default

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            minecraftServer = server
            // remove overworld spawn tickets
            minecraftServer.overworld.chunkManager.removePersistentTickets()

            PlaceStructure.placeStructure(lobbyWorld, BlockPos(-24, -1, -24), Identifier.of("bingo", "lobby")!!)
            BingoGame.spawnMenu(Vec3d(-4.0, 3.0, -4.0))
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

                BingoGame.updateTimer()
            }

            // TODO: move this elsewhere?
            for (player in server.playerManager.playerList) {
                BingoGame.giveMapCardItem(player)
            }
        }

        PlayerEvents.onPlayerJoin { player ->
            BingoGame.updateGameMode(player)
            BingoGame.giveMapCardItem(player)

            // if in pregame, move to the lobby world
            if (BingoGame.state == GameState.PREGAME) {
                player.setSpawnPoint(lobbyWorld.registryKey, BlockPos.ORIGIN, 180f, true, false)
                player.teleport(lobbyWorld, 0.5, 0.0, 0.5, 180f, 0f)
            }
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { player, _, _ ->
            // give player a new map, if they don't have one?
            BingoGame.giveMapCardItem(player)

            // if in pregame, move to the lobby world
            if (BingoGame.state == GameState.PREGAME) {
                player.setSpawnPoint(lobbyWorld.registryKey, BlockPos.ORIGIN, 180f, true, false)
                player.teleport(lobbyWorld, 0.5, 0.0, 0.5, 180f, 0f)
            }
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            val itemStack = player.getStackInHand(hand)
            val team = BingoTeam.fromPlayer(player) ?: return@register TypedActionResult.pass(itemStack)

            // if the player has an item they could use in their other hand (e.g. food or a block), don't open the map
            // - this prevents a de-sync bug where the player could use both items at once
            val otherHandStack = player.getStackInHand(when (hand) {
                Hand.MAIN_HAND -> Hand.OFF_HAND
                else -> Hand.MAIN_HAND
            })

            // if the item is the player's bingo map, open the item details chest
            if (itemStack.item == Items.FILLED_MAP && itemStack.nbt?.getInt("map") == team.mapId && otherHandStack.isEmpty) {
                CardService.showInventory(player, BingoGame.card)
                return@register TypedActionResult.success(itemStack, true)
            }

            TypedActionResult.pass(itemStack)
        }
    }

}
