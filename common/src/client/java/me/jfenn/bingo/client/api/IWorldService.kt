package me.jfenn.bingo.client.api

import net.minecraft.server.MinecraftServer

interface IWorldService {

    fun createBingoWorld()

    fun isBingoWorld(server: MinecraftServer): Boolean

    fun deleteSave(server: MinecraftServer)

    fun disconnect()

    fun disconnectAndReopen()

    fun restartServer()

}