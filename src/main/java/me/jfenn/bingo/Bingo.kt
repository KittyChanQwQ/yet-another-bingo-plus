package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.card.GeneratorTier
import me.jfenn.bingo.event.PlayerEvents
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.menu.MenuService
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.*
import net.minecraft.util.Unit
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import org.slf4j.LoggerFactory
import java.util.*

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

    private val log = LoggerFactory.getLogger(this::class.java)

    init {
        WorldDeleter.invoke()
    }

    override fun onInitialize() {
        log.info("$MOD_ID mod initialized (main)")
        MapRenderService.validateItems()

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            minecraftServer = server
            // remove overworld spawn tickets
            minecraftServer.overworld.chunkManager.removePersistentTickets()

            MenuService.spawn(BingoGame.config)

            // boss bar needs to be initialized here, to fix client desync
            // - where are these stored? previous bossbars are somehow persisted, even though world data isn't
            BingoGame.timeBar
            BingoGame.state = GameState.PREGAME
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            BingoGame.updateInventory()
            BingoGame.updateTimer()

            for (player in server.playerManager.playerList) {
                BingoGame.giveMapCardItem(player)
                BingoGame.giveElytra(player)
            }
        }

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, type ->
            if (BingoGame.state != GameState.PLAYING) return@register true
            val team = player.scoreboardTeam ?: return@register true

            val messageText = Text.empty()
                .append(Text.literal("TEAM ${player.entityName}").formatted(team.color))
                .append(Text.literal(": "))
                .append(message.content)

            // broadcast message to team
            for (otherPlayer in minecraftServer.playerManager.playerList) {
                if (otherPlayer.isTeamPlayer(team)) otherPlayer.sendMessage(messageText)
            }

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

        val itemCooldown = mutableMapOf<UUID, Int>()
        UseItemCallback.EVENT.register { player, world, hand ->
            val itemStack = player.getStackInHand(hand)
            val team = BingoTeam.fromPlayer(player) ?: return@register TypedActionResult.pass(itemStack)

            // if the player has an item they could use in their other hand (e.g. food or a block), don't open the map
            // - this prevents a de-sync bug where the player could use both items at once
            val otherHandStack = player.getStackInHand(when (hand) {
                Hand.MAIN_HAND -> Hand.OFF_HAND
                else -> Hand.MAIN_HAND
            })

            // ensure the player waits at least 10 ticks before using the map
            // - if the use button is held, this will continually update the cooldown, and never exit this state until released
            val isInCooldown = minecraftServer.ticks < (itemCooldown[player.uuid] ?: 0) + 10

            // if the item is the player's bingo map, open the item details chest
            if (itemStack.item == Items.FILLED_MAP && itemStack.nbt?.getInt("map") == team.mapId && otherHandStack.isEmpty && !isInCooldown) {
                CardService.showInventory(player, BingoGame.card)
                return@register TypedActionResult.success(itemStack, true)
            }

            itemCooldown[player.uuid] = minecraftServer.ticks
            TypedActionResult.pass(itemStack)
        }

        UseBlockCallback.EVENT.register { player, world, hand, hit ->
            itemCooldown[player.uuid] = minecraftServer.ticks
            ActionResult.PASS
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            for (command in commands()) {
                dispatcher.register(command)
            }
        }
    }

}
