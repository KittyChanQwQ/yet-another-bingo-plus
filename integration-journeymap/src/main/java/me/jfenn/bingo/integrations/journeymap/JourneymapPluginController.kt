package me.jfenn.bingo.integrations.journeymap

import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.ScopeStarted
import me.jfenn.bingo.plugin.IBingoInternalPlugin
import org.koin.core.Koin

internal class JourneymapPluginController : IBingoInternalPlugin {
    override fun initialize(koin: Koin) {
        val environment = koin.get<IModEnvironment>()
        val events = koin.get<IEventBus>()

        if (environment.isModLoaded("journeymap")) {
            events.register(ScopeStarted) {
                JourneymapPlugin(it.scope.get(), it.scope.get(), it.scope.get())
            }
        }
    }
}