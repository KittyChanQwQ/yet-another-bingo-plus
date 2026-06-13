package me.jfenn.bingo.integrations.journeymap

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.plugin.IBingoInternalPlugin
import org.koin.core.Koin

internal class JourneymapPluginController : IBingoInternalPlugin {

    override fun initialize(
        koin: Koin,
    ) {
        val environment = koin.get<IModEnvironment>()

        if (
            // This plugin is only required client-side. Trying to load the ClientPacketEvents on the server causes a crash.
            environment.envType == IModEnvironment.EnvType.CLIENT &&
            environment.isModLoaded("journeymap")
        ) {
            val eventBus = koin.get<IEventBus>()
            val events = koin.get<ClientPacketEvents>()
            val plugin = JourneymapPlugin()

            eventBus.register(events.gameResetV1) {
                plugin.resetJourneymapData()
            }
        }

    }
}