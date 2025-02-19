package me.jfenn.bingo.integrations.waystones

import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent
import net.blay09.mods.waystones.core.PlayerWaystoneManager
import net.blay09.mods.waystones.core.WaystoneManagerImpl
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger

internal class WaystonesPlugin(
    private val log: Logger,
    private val server: MinecraftServer,
    events: IEventBus,
) {
    init {
        events.register(GameResetEvent) {
            log.info("[WaystonesPlugin] Game resetting...")

            val manager = WaystoneManagerImpl.get(server)
            for (waystone in manager.waystones.toList()) {
                log.info("[WaystonesPlugin] Removing waystone ${waystone.waystoneUid}")
                manager.removeWaystone(waystone)
                PlayerWaystoneManager.removeKnownWaystone(server, waystone)
            }
        }
    }
}