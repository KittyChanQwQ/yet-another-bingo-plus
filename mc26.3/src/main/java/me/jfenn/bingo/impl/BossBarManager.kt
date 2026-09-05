package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IBossBar
import me.jfenn.bingo.platform.IBossBarManager
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.text.IText
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.bossevents.CustomBossEvent
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.world.BossEvent
import java.lang.ref.WeakReference
import java.util.*

internal class BossBarManager(
    private val server: MinecraftServer,
) : IBossBarManager {
    companion object {
        @get:JvmName("getServerBossBars")
        internal val serverBossBars = Collections.synchronizedList(
            mutableListOf<WeakReference<ServerBossEvent>>()
        )
    }

    override fun get(id: String): IBossBar? {
        return server.customBossEvents.get(Identifier.parse(id))
            ?.let { BossBarImpl(it) }
    }

    override fun remove(bossBar: IBossBar) {
        require(bossBar is BossBarImpl)
        bossBar.clearPlayers()
        (bossBar.bossBar as? CustomBossEvent)
            ?.let { server.customBossEvents.remove(it) }
    }

    override fun add(id: String, title: Component): IBossBar {
        val bossBar = server.customBossEvents.create(server.overworld().random, Identifier.parse(id), title)
        return BossBarImpl(bossBar)
    }

    override fun list(): List<IBossBar> {
        return server.customBossEvents.events
            .map { BossBarImpl(it) }
            .plus(
                serverBossBars
                    .mapNotNull { it.get() }
                    .map { BossBarImpl(it) }
            )
    }
}

internal class BossBarImpl(
    internal val bossBar: BossEvent,
): IBossBar {
    private val commandBossBar: CustomBossEvent? = bossBar as? CustomBossEvent
    private val serverBossBar: ServerBossEvent? = bossBar as? ServerBossEvent

    override val id: String?
        get() = commandBossBar?.id?.toString()

    override var name: IText
        get() = TextImpl(bossBar.name.copy())
        set(value) { bossBar.name = value.value }

    override var color: IBossBar.Color
        get() = IBossBar.Color.WHITE
        set(value) {
            bossBar.color = when (value) {
                IBossBar.Color.WHITE -> BossEvent.BossBarColor.WHITE
            }
        }

    override var style: IBossBar.Style
        get() = IBossBar.Style.PROGRESS
        set(value) {
            bossBar.overlay = when (value) {
                IBossBar.Style.PROGRESS -> BossEvent.BossBarOverlay.PROGRESS
            }
        }

    override var value: Int
        get() = commandBossBar?.value() ?: (bossBar.progress * 100).toInt()
        set(value) {
            commandBossBar?.setValue(value)
                ?: bossBar.apply { progress = value / 100f }
        }
    override var maxValue: Int
        get() = commandBossBar?.max() ?: 100
        set(value) {
            commandBossBar?.setMax(value)
                ?: throw UnsupportedOperationException("Cannot set max value on a non-command bossbar")
        }

    override fun addPlayer(player: IPlayerHandle) {
        require(player is PlayerHandle)
        commandBossBar?.addPlayer(player.player)
        serverBossBar?.addPlayer(player.player)
    }

    override fun removePlayer(player: IPlayerHandle) {
        require(player is PlayerHandle)
        commandBossBar?.removePlayer(player.player)
        serverBossBar?.removePlayer(player.player)
    }

    override fun clearPlayers() {
        commandBossBar?.removeAllPlayers()
        serverBossBar?.removeAllPlayers()
    }
}
