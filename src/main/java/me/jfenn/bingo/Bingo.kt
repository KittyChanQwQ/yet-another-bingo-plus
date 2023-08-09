package me.jfenn.bingo

import com.mojang.brigadier.builder.LiteralArgumentBuilder.*
import me.jfenn.bingo.card.WinCondition
import me.jfenn.bingo.event.PlayerEvents
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.menu.MenuEntity
import me.jfenn.bingo.menu.PlaceStructure
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.PlaceCommand
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.command.SetWorldSpawnCommand
import net.minecraft.server.command.SpreadPlayersCommand
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.Unit
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.Heightmap
import net.minecraft.world.gen.structure.Structure

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
                player.setSpawnPoint(lobbyWorld.registryKey, BlockPos.ORIGIN, 0f, true, false)
                player.teleport(lobbyWorld, 0.5, 0.0, 0.5, 0f, 0f)
            }
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { player, _, _ ->
            // give player a new map, if they don't have one?
            BingoGame.giveMapCardItem(player)

            // if in pregame, move to the lobby world
            if (BingoGame.state == GameState.PREGAME) {
                player.setSpawnPoint(lobbyWorld.registryKey, BlockPos.ORIGIN, 0f, true, false)
                player.teleport(lobbyWorld, 0.5, 0.0, 0.5, 0f, 0f)
            }
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            val itemStack = player.getStackInHand(hand)
            val team = BingoTeam.fromPlayer(player) ?: return@register TypedActionResult.pass(itemStack)

            // if the player has an item they could use in their offhand (e.g. food or a block), don't open the map
            // - this prevents a de-sync bug where the player could use both items at once
            val isUsingOffhand = !player.offHandStack.isEmpty && hand != Hand.OFF_HAND

            // if the item is the player's bingo map, open the item details chest
            if (itemStack.item == Items.FILLED_MAP && itemStack.nbt?.getInt("map") == team.mapId && !isUsingOffhand) {
                CardService.showInventory(player, BingoGame.card)
                return@register TypedActionResult.success(itemStack, true)
            }

            TypedActionResult.pass(itemStack)
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
