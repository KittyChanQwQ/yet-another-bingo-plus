package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.block.Blocks
import net.minecraft.util.ActionResult

class PlayerSpectatorController(
    events: ScopedEvents,
    private val state: BingoState,
) {

    init {
        val allowedBlockSet = setOf(Blocks.BARREL, Blocks.CHEST)
        events.onUseBlock {
            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (state.state == GameState.PLAYING && it.player.isCreative) {
                val blockState = it.player.world.getBlockState(it.hit.blockPos)
                if (!allowedBlockSet.contains(blockState.block)) {
                    return@onUseBlock ActionResult.FAIL
                }
            }
            null
        }

        events.onAttackBlock {
            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (state.state == GameState.PLAYING && it.player.isCreative) {
                return@onAttackBlock ActionResult.FAIL
            }

            null
        }

        events.onAttackEntity {
            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (state.state == GameState.PLAYING && it.player.isCreative) {
                return@onAttackEntity ActionResult.FAIL
            }

            null
        }
    }

}