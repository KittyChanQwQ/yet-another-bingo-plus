package me.jfenn.bingo.integrations.xaeros

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.plugin.IBingoInternalPlugin
import org.koin.core.Koin

internal class XaerosPluginController : IBingoInternalPlugin {

    override fun initialize(
        koin: Koin,
    ) {
        val environment = koin.get<IModEnvironment>()
        val minimapLoaded = environment.isModLoaded("xaerominimap")
        val worldmapLoaded = environment.isModLoaded("xaeroworldmap")

        if (
            // This plugin is only required client-side. Trying to load the ClientPacketEvents on the server causes a crash.
            environment.envType == IModEnvironment.EnvType.CLIENT &&
            (minimapLoaded || worldmapLoaded)
        ) {
            val eventBus = koin.get<IEventBus>()
            val events = koin.get<ClientPacketEvents>()
            val plugin = XaerosPlugin()

            eventBus.register(events.gameResetV1) {
                // If you don't have worldmap loaded, resetting the map is kind of pointless.
                // All the area you can view in the minimap gets reloaded all the time anyhow.
                if (worldmapLoaded) {
                    plugin.resetXaerosMapData()
                }

                // Minimap deals with Waypoints. If only worldmap is loaded, there are no waypoints to delete.
                if (minimapLoaded) {
                    plugin.resetXaerosWaypointData()
                }
            }
        }

    }
}