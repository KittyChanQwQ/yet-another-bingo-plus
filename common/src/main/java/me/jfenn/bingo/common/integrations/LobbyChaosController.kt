package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.hasPermission
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult

class LobbyChaosController(
    events: ScopedEvents,
    val config: BingoConfig,
    val state: BingoState,
) {

    private fun shouldPreventLobbyChaos(player: PlayerEntity): Boolean {
        return config.preventLobbyChaos &&
                state.state == GameState.PREGAME &&
                !player.hasPermission()
    }

    init {
        events.onUseBlock {
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