package me.jfenn.bingo.integrations.chunky

import me.jfenn.bingo.api.BingoEvents
import me.jfenn.bingo.api.IBingoApi
import me.jfenn.bingo.api.data.BingoGameStatus
import me.jfenn.bingo.platform.ICommandRunner
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import java.io.Closeable

class ChunkyController(
    api: IBingoApi,
    private val server: MinecraftServer,
    private val config: ChunkyConfig,
    private val commandRunner: ICommandRunner,
    private val log: Logger,
) : Closeable {

    init {
        log.info("Integrations: Chunky is installed!")
    }

    private fun startPregen() {
        try {
            config.chunkyWorlds.forEach { (world, size) ->
                if (size > 0) {
                    commandRunner.runSilentCommand(server, "chunky world $world")
                    commandRunner.runSilentCommand(server, "chunky radius $size")
                    commandRunner.runSilentCommand(server, "chunky start")
                }
            }
            commandRunner.runSilentCommand(server, "chunky continue")
        } catch (e: Throwable) {
            log.error("Error starting chunky pre-generation:", e)
        }
    }

    private fun cancelTasks() {
        try {
            commandRunner.runSilentCommand(server, "chunky cancel")
            commandRunner.runSilentCommand(server, "chunky confirm")
        } catch (e: Throwable) {
            log.error("Error cancelling chunky pre-generation:", e)
        }
    }

    private val listeners = listOf(
        BingoEvents.GAME_RESET.register {
            startPregen()
        },
        BingoEvents.GAME_STARTING.register {
            cancelTasks()
        },
        BingoEvents.GAME_STARTED.register {
            cancelTasks()
        }
    )

    init {
        if (api.game.status == BingoGameStatus.PLAYING)
            cancelTasks()
    }

    override fun close() {
        listeners.forEach { it.close() }
    }

}