package me.jfenn.bingo.integrations.journeymap

import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.plugin.IBingoInternalPlugin
import org.koin.core.Koin

internal class JourneymapPluginController(
    private val events: ClientPacketEvents
) : IBingoInternalPlugin {

    // TODO: This currently does not build, because the serviceloader expects a parameterless constructor

    override fun initialize(
        koin: Koin,
    ) {
        val environment = koin.get<IModEnvironment>()
        val eventBus = koin.get<IEventBus>()

        if (environment.isModLoaded("journeymap")) {
            val plugin = JourneymapPlugin()

            eventBus.register(events.gameResetV1) {
                plugin.resetJourneymapData()
            }
        }
    }
}