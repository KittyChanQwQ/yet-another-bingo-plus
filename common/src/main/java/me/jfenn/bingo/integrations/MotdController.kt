package me.jfenn.bingo.integrations

import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.state.GameState

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