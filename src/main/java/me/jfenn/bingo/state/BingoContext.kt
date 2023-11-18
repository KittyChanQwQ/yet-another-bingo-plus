package me.jfenn.bingo.state

import me.jfenn.bingo.LOBBY_WORLD_ID
import me.jfenn.bingo.integrations.BossBarController
import me.jfenn.bingo.integrations.ScoreboardController
import me.jfenn.bingo.menu.MenuInstance
import net.minecraft.server.MinecraftServer

class BingoContext(
    val server: MinecraftServer,
    val state: BingoState,
) {

    val config get() = state.config

    val lobbyWorld by lazy {
        server.worlds.find { it.registryKey.value == LOBBY_WORLD_ID }!!
    }

    val bossBar by lazy { BossBarController.getBossbar(server) }

    val scoreboards by lazy { ScoreboardController.getScoreboards(server) }

    var menu: MenuInstance? = null

    operator fun component1() = server
    operator fun component2() = state
}
