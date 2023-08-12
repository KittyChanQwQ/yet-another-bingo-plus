package me.jfenn.bingo

import me.jfenn.bingo.event.PlayerEvents
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.menu.MenuService
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.util.Unit
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos

const val MOD_ID = "bingo"

lateinit var minecraftServer: MinecraftServer

val LOBBY_WORLD_ID = Identifier.of("bingo", "lobby")
val lobbyWorld: ServerWorld by lazy {
    minecraftServer.worlds
        .find { it.registryKey.value == LOBBY_WORLD_ID }!!
        .also { world ->
            // always load the spawn chunks
            world.chunkManager.addTicket(ChunkTicketType.START, ChunkPos(0, 0), 11, Unit.INSTANCE)

            // this doesn't work because world border settings only take effect when applied to the overworld
            // world.worldBorder.apply {
            //     setCenter(0.0, 0.0)
            //     size = 48.0
            // }
        }
}

object Bingo : ModInitializer {

    override fun onInitialize() {
        println("$MOD_ID mod initialized (main)")
        WorldDeleter.invoke()

        // TODO:
        // ? spawn area worldborder
        // - spawn area saturation
        // - spawn team picker, fix hitboxes, give interaction feedback
        // - spawn team shuffle tool
        // - setting for pvp mode
        // - setting for granting recipes
        // - collect bingo image resources from the minecraft jar (as a gradle task?)

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            minecraftServer = server
            // remove overworld spawn tickets
            minecraftServer.overworld.chunkManager.removePersistentTickets()

            MenuService.spawn()
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            if (BingoGame.state == GameState.PLAYING) {
                for (player in server.playerManager.playerList) {
                    val team = BingoTeam.fromPlayer(player) ?: continue

                    // check inventory for changes
                    for (item in BingoGame.card.entries) if (!item.hasAchieved(team)) {
                        val hasItem = player.inventory.containsAny { item.canSatisfy(it) }
                                || item.canSatisfy(player.currentScreenHandler.cursorStack)
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

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, type ->
            if (BingoGame.state != GameState.PLAYING) return@register true
            val team = BingoTeam.fromPlayer(player) ?: return@register true

            // broadcast message to team
            team.broadcastMessage(
                Text.empty()
                    .append(Text.literal("TEAM ${player.entityName}").formatted(team.formatting))
                    .append(Text.literal(": "))
                    .append(message.content)
            )

            false
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

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            for (command in commands()) {
                dispatcher.register(command)
            }
        }
    }

}
