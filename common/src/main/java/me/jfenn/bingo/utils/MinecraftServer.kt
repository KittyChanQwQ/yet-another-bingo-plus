package me.jfenn.bingo.utils

import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandOutput

fun MinecraftServer.runSilentCommand(command: String) {
    commandManager.executeWithPrefix(
        commandSource.withOutput(CommandOutput.DUMMY),
        command,
    )
}
