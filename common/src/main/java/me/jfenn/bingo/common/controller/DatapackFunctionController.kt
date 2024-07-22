package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.ICommandRunner
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

class DatapackFunctionController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val commandRunner: ICommandRunner,
): BingoComponent() {

    private val commandFiles = GameState.entries.associateWith { state ->
        Config.readStream(
            file = "commands/on_${state.name.lowercase()}.mcfunction",
            shouldWriteDefault = true,
            default = { IOUtils.toInputStream("", StandardCharsets.UTF_8) }
        ).use { stream ->
            IOUtils.toString(stream, StandardCharsets.UTF_8)
        }
    }

    init {
        // run "#yet-another-minecraft-bingo:on_playing" functions when the game state is changed
        events.onStateChange { (_, to) ->
            kotlin.runCatching {
                commandRunner.runSilentCommand(server, "function #${MOD_ID_BINGO}:on_${to.name.lowercase()}")
            }

            kotlin.runCatching {
                val commands = commandFiles[to]
                    ?.split(Regex("\\R"))
                    ?.map { it.removePrefix("/").trim() }
                    ?.filter { it.isNotBlank() && !it.startsWith("#") }
                    ?: emptyList()

                for (command in commands) {
                    commandRunner.runSilentCommand(server, command)
                }
            }
        }
    }
}