package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IBossBar
import me.jfenn.bingo.platform.IBossBarManager
import me.jfenn.bingo.platform.IPlayerHandle
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.CommandBossBar
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Identifier

internal class BossBarManager(
    private val server: MinecraftServer,
) : IBossBarManager {
    override fun get(id: String): IBossBar? {
        return server.bossBarManager.get(Identifier(id))
            ?.let { BossBarImpl(it) }
    }

    override fun remove(bossBar: IBossBar) {
        require(bossBar is BossBarImpl)
        server.bossBarManager.remove(bossBar.bossBar)
    }

    override fun add(id: String, title: Text): IBossBar {
        val bossBar = server.bossBarManager.add(Identifier(id), title)
        return BossBarImpl(bossBar)
    }

    override fun list(): List<IBossBar> {
        return server.bossBarManager.all
            .map { BossBarImpl(it) }
    }
}

internal class BossBarImpl(
    internal val bossBar: CommandBossBar,
): IBossBar {
    override val id: String
        get() = bossBar.id.toString()

    override var name: Text by bossBar::name
    override var color: IBossBar.Color
        get() = IBossBar.Color.WHITE
        set(value) {
            bossBar.color = when (value) {
                IBossBar.Color.WHITE -> BossBar.Color.WHITE
            }
        }

    override var style: IBossBar.Style
        get() = IBossBar.Style.PROGRESS
        set(value) {
            bossBar.style = when (value) {
                IBossBar.Style.PROGRESS -> BossBar.Style.PROGRESS
            }
        }

    override var value: Int by bossBar::value
    override var maxValue: Int by bossBar::maxValue

    override fun addPlayer(player: IPlayerHandle) {
        require(player is PlayerHandle)
        bossBar.addPlayer(player.player)
    }

    override fun removePlayer(player: IPlayerHandle) {
        require(player is PlayerHandle)
        bossBar.removePlayer(player.player)
    }

    override fun clearPlayers() {
        bossBar.clearPlayers()
    }
}
