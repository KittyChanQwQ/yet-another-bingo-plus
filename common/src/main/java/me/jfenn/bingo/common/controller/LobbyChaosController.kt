package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.ChatMessages
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.block.Blocks
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

class LobbyChaosController(
    events: ScopedEvents,
    private val config: BingoConfig,
    private val state: BingoState,
    private val permissions: PermissionsIntegration,
) {

    private fun shouldPreventLobbyChaos(player: ServerPlayerEntity): Boolean {
        return config.preventLobbyChaos &&
                state.state == GameState.PREGAME &&
                !permissions.hasPermission(player, Permission.BYPASS_CHAOS_PREVENTION)
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
            val player = it.player as? ServerPlayerEntity ?: return@onUseBlock null

            if (config.preventLobbyChaos && state.state == GameState.PREGAME) {
                val blockState = player.world.getBlockState(it.hit.blockPos)
                if (blockState.block in hitBlockSet) {
                    ChatMessages.broadcastLobbyMuted(player, overlay = true)
                }
            }

            // If preventLobbyChaos=true, prevent players from accessing the brewing stand
            // (levitation potions are annoying...)
            if (shouldPreventLobbyChaos(player)) {
                val blockState = player.world.getBlockState(it.hit.blockPos)
                if (blockState.block == Blocks.BREWING_STAND)
                    return@onUseBlock ActionResult.FAIL
            }

            null
        }
    }

}