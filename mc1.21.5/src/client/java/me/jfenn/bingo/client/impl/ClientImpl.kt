package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.IClientPlayer
import net.minecraft.client.MinecraftClient

class ClientImpl : IClient {
    private val client = MinecraftClient.getInstance()

    override val player: IClientPlayer = PlayerImpl()

    override val isInSingleplayer: Boolean
        get() = client.isInSingleplayer

    inner class PlayerImpl : IClientPlayer {
        override fun sendCommand(command: String) {
            client.player?.networkHandler?.sendCommand(
                command.removePrefix("/")
            )
        }
    }
}