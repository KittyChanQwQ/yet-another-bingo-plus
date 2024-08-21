package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.ICommandRunner
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

class DatapackFunctionService(
    private val server: MinecraftServer,
    private val commandRunner: ICommandRunner,
): BingoComponent() {

    private val supportedEvents = setOf(
        GameState.PREGAME,
        GameState.LOADING,
        GameState.COUNTDOWN,
        GameState.PLAYING,
        GameState.POSTGAME
    )

    private val commandFiles = supportedEvents.associateWith { state ->
        Config.readStream(
            file = "commands/on_${state.name.lowercase()}.mcfunction",
            shouldWriteDefault = true,
            default = { IOUtils.toInputStream("", StandardCharsets.UTF_8) }
        ).use { stream ->
            IOUtils.toString(stream, StandardCharsets.UTF_8)
        }
    }

    /**
     * run "#yet-another-minecraft-bingo:on_playing" functions when the game state is changed
     */
    fun runStateChange(state: GameState) {
        if (state !in supportedEvents)
            return

        kotlin.runCatching {
            commandRunner.runSilentCommand(server, "function #${MOD_ID_BINGO}:on_${state.name.lowercase()}")
        }

        kotlin.runCatching {
            val commands = commandFiles[state]
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