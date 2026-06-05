package me.jfenn.bingo.integrations.journeymap

import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger

internal class JourneymapPlugin(
    private val log: Logger,
    private val server: MinecraftServer,
    events: IEventBus,
) {
    init {
        events.register(GameResetEvent) {
            log.info("[JourneymapPlugin] Game resetting...")


        }
    }
}