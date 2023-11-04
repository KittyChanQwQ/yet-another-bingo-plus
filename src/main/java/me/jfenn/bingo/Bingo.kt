package me.jfenn.bingo

import me.jfenn.bingo.map.MapItemHandler
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.menu.MenuService
import me.jfenn.bingo.spawn.SpawnService
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Unit
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.GameRules
import org.slf4j.LoggerFactory

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

            // hide spammy messages in the lobby
            minecraftServer.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, minecraftServer)
            minecraftServer.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, minecraftServer)

            MenuService.spawn(BingoGame.config)

            // boss bar needs to be initialized here, to fix client desync
            // - where are these stored? previous bossbars are somehow persisted, even though world data isn't
            BingoGame.timeBar
            BingoGame.state = GameState.PREGAME
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            BingoGame.updateInventory()

            if (server.ticks % 20 == 0)
                BingoGame.updateTimer()

            for (player in server.playerManager.playerList) {
                BingoGame.giveMapCardItem(player)
                BingoGame.giveElytra(player)
                BingoGame.giveSpectatorMapItems(player)
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

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            BingoGame.updateGameMode(player)
            BingoGame.giveMapCardItem(player)

            // if in pregame, move to the lobby world
            if (BingoGame.state == GameState.PREGAME) {
                val spawnPos = MenuService.spawnPos
                player.setSpawnPoint(lobbyWorld.registryKey, spawnPos, 180f, true, false)
                player.teleport(lobbyWorld, spawnPos.x + 0.5, spawnPos.y.toDouble(), spawnPos.z + 0.5, 180f, 0f)
            }

        }

        ServerPlayerEvents.AFTER_RESPAWN.register { _, player, _ ->
            // give player a new map, if they don't have one?
            BingoGame.giveMapCardItem(player)

            // if in pregame, move to the lobby world
            if (BingoGame.state == GameState.PREGAME) {
                val spawnPos = MenuService.spawnPos
                player.setSpawnPoint(lobbyWorld.registryKey, spawnPos, 180f, true, false)
                player.teleport(lobbyWorld, spawnPos.x + 0.5, spawnPos.y.toDouble(), spawnPos.z + 0.5, 180f, 0f)
            }

            // if playing, give the player new spawn equipment
            if (
                BingoGame.state == GameState.PLAYING
                && BingoGame.config.refillPlayerKitOnRespawn
                // if KEEP_INVENTORY is on, we don't need to replace these items
                && !minecraftServer.gameRules.get(GameRules.KEEP_INVENTORY).get()
            ) {
                SpawnService.giveSpawnEquipment(BingoGame.config, listOf(player))
            }
        }

        MapItemHandler.registerCallbacks()

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            for (command in commands()) {
                dispatcher.register(command)
            }
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            println("BINGO! stopping...")
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            println("BINGO! stopped!")
        }
    }

}
