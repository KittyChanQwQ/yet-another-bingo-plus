package me.jfenn.bingo.common.integrations

import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.GameState

object MotdController {

    fun updateMotd(ctx: BingoContext) {
        val state = ctx.state.state
        ctx.server.setMotd("${state.color}[${state.motd}]§f Minecraft BINGO")
    }

    fun init() {
        for (state in GameState.entries) {
            state.onEnter {
                updateMotd(this)
            }
        }
    }

}