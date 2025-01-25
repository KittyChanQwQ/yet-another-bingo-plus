package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.config.TrackedFileService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.ICommandRunner
import net.minecraft.server.MinecraftServer

internal class DatapackFunctionService(
    private val server: MinecraftServer,
    private val commandRunner: ICommandRunner,
    private val trackedFileService: TrackedFileService,
): BingoComponent() {

    companion object {
        val SUPPORTED_EVENTS = arrayOf(
            GameState.PREGAME,
            GameState.STARTING,
            GameState.COUNTDOWN,
            GameState.PLAYING,
            GameState.POSTGAME
        )

        fun filePath(state: GameState) = "$MOD_ID_BINGO/commands/on_${state.name.lowercase()}.mcfunction"
    }

    private var commandFiles = emptyMap<GameState, String>()

    private fun loadCommandFile(state: GameState): String {
        val pathStr = filePath(state)
        val path = Config.configDir.resolve(pathStr)
        val resource = this::class.java
            .getResourceAsStream("/$pathStr")
            ?.use { stream -> stream.bufferedReader().use { it.readText() } }
            .orEmpty()

        return trackedFileService.readTextFileOrResource(
            path = path,
            resource = resource,
        ).config.orEmpty()
    }

    fun loadCommands() {
        commandFiles = SUPPORTED_EVENTS.associateWith { loadCommandFile(it) }
    }

    /**
     * run "#yet-another-minecraft-bingo:on_playing" functions when the game state is changed
     */
    fun runStateChange(state: GameState) {
        if (state !in SUPPORTED_EVENTS)
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