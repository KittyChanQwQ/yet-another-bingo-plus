package me.jfenn.bingo.api

import net.minecraft.server.MinecraftServer

interface ICommandRunner {
    fun runSilentCommand(server: MinecraftServer, cmd: String)
}