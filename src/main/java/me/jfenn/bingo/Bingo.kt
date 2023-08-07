package me.jfenn.bingo

import com.mojang.brigadier.builder.LiteralArgumentBuilder.*
import me.jfenn.bingo.data.BingoCard
import me.jfenn.bingo.data.BingoTeam
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.map.BingoMapService
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult

const val MOD_ID = "bingo"

enum class GameState {
    PREGAME,
    PLAYING,
    POSTGAME,
}

lateinit var minecraftServer: MinecraftServer

object Bingo : ModInitializer {

    override fun onInitialize() {
        println("$MOD_ID mod initialized (main)")

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            minecraftServer = server
        }

        var state = GameState.PREGAME
        var card: BingoCard? = null

        ServerTickEvents.START_SERVER_TICK.register { server ->
            val shouldUpdateMaps = mutableSetOf<BingoTeam>()

            if (state == GameState.PLAYING) card?.let { card ->
                for (player in server.playerManager.playerList) {
                    val team = BingoTeam.fromPlayer(player) ?: continue

                    // check inventory for changes
                    for (item in card.entries) {
                        val hasItem = player.inventory.containsAny { it.item == item.item() }
                        if (item.setAchieved(team, hasItem)) {
                            // play the doink noise
                            SoundService.playTileCollected(team)

                            // update the team map
                            shouldUpdateMaps.add(team)
                        }
                    }
                }
            }

            // update map renders if needed
            card?.let { card ->
                for (team in BingoTeam.values()) if (shouldUpdateMaps.contains(team)) {
                    BingoMapService.update(team, card)
                }
            }

            // TODO: move this elsewhere?
            for (player in server.playerManager.playerList) {
                val hasMap = player.inventory.containsAny { it.item == Items.FILLED_MAP }
                if (!hasMap) {
                    BingoTeam.fromPlayer(player)
                        ?.let { team -> BingoMapService.createMapItem(team) }
                        ?.also { player.giveItemStack(it) }
                }
            }
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            val team = BingoTeam.fromPlayer(player) ?: return@register TypedActionResult.pass(null)

            val itemStack = player.getStackInHand(hand)

            // if the item is the player's bingo map, open the item details chest
            if (itemStack.item == Items.FILLED_MAP && itemStack.nbt?.getInt("map") == team.mapId) {
                card?.let { CardService.showInventory(player, it) }
                return@register TypedActionResult.consume(null)
            }

            TypedActionResult.pass(null)
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            dispatcher.register(literal<ServerCommandSource?>("start").executes { ctx ->

                // generate a new card for the round
                card = CardService.generate(listOf(2, 6, 9, 6, 2))

                // create the card map images
                for (team in BingoTeam.values()) {
                    card?.let { BingoMapService.update(team, it) }
                }

                // TODO: sort teams evenly
                for (player in ctx.source.server.playerManager.playerList) {
                    // TODO: temporary, remove this
                    if (BingoTeam.fromPlayer(player) == null)
                        BingoTeam.BLUE.addPlayer(player)

                    // reset player info
                    player.clearStatusEffects()
                    player.inventory.clear()

                    // give each player their team's card
                    BingoTeam.fromPlayer(player)
                        ?.let { team -> BingoMapService.createMapItem(team) }
                        ?.also { item -> player.giveItemStack(item) }
                }

                state = GameState.PLAYING
                1
            })
        }
    }

}
