package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.ChatMessages
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.hasPermission
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ActionResult

class LobbyChaosController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val config: BingoConfig,
    private val state: BingoState,
) {

    private fun shouldPreventLobbyChaos(player: PlayerEntity): Boolean {
        return config.preventLobbyChaos &&
                state.state == GameState.PREGAME &&
                !player.hasPermission()
    }

    private val hitBlockSet = setOf(
        Blocks.BARREL, Blocks.CHEST, Blocks.SWEET_BERRY_BUSH, Blocks.DECORATED_POT
    )

    init {
        events.onPlayerJoin { player ->
            if (config.preventLobbyChaos && state.state == GameState.PREGAME) {
                ChatMessages.broadcastLobbyMuted(player)
            }
        }

        events.onUseBlock {
            if (config.preventLobbyChaos && state.state == GameState.PREGAME) {
                val blockState = it.player.world.getBlockState(it.hit.blockPos)
                if (blockState.block in hitBlockSet) {
                    server.playerManager.getPlayer(it.player.uuid)
                        ?.let { serverPlayer -> ChatMessages.broadcastLobbyMuted(serverPlayer, overlay = true) }
                }
            }

            // If preventLobbyChaos=true, prevent players from accessing the brewing stand
            // (levitation potions are annoying...)
            if (shouldPreventLobbyChaos(it.player)) {
                val blockState = it.player.world.getBlockState(it.hit.blockPos)
                if (blockState.block == Blocks.BREWING_STAND)
                    return@onUseBlock ActionResult.FAIL
            }

            null
        }
    }

}