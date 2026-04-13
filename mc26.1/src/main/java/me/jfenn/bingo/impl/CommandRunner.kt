package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.ICommandRunner
import net.minecraft.commands.CommandSource
import net.minecraft.server.MinecraftServer

class CommandRunner : ICommandRunner {
    override fun runSilentCommand(server: MinecraftServer, cmd: String) {
        val parsed = server.commands.dispatcher.parse(cmd, server.createCommandSourceStack().withSource(CommandSource.NULL))
        server.commands.performCommand(parsed, cmd)
    }
}