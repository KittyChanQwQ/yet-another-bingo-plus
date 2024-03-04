package me.jfenn.bingo.common.utils

import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandOutput

fun MinecraftServer.runSilentCommand(cmd: String) {
    commandManager.executeWithPrefix(commandSource.withOutput(CommandOutput.DUMMY), cmd)
}
