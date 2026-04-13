package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.impl.draw.FontImpl
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.IClientPlayer
import me.jfenn.bingo.client.platform.renderer.IFont
import me.jfenn.bingo.platform.text.IText
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.joml.Vector2i
import java.util.concurrent.Executor

class ClientImpl : IClient {
    private val client = Minecraft.getInstance()

    override val font: IFont
        get() = FontImpl(client.font)

    override val player: IClientPlayer = PlayerImpl()

    override val isInSingleplayer: Boolean
        get() = client.isLocalServer

    override val isPaused: Boolean
        get() = client.isPaused

    override val mouse: Vector2i
        get() {
            val mouse = client.mouseHandler
            return Vector2i(
                (mouse.xpos() * client.window.guiScaledWidth.toDouble() / client.window.width.toDouble()).toInt(),
                (mouse.ypos() * client.window.guiScaledHeight.toDouble() / client.window.height.toDouble()).toInt(),
            )
        }

    override var screen: Screen?
        get() = client.screen
        set(value) { client.setScreen(value) }

    override fun execute(callback: () -> Unit) {
        client.execute(callback)
    }

    override val executor: Executor = client

    inner class PlayerImpl : IClientPlayer {
        override fun sendHotbarMessage(text: IText) {
            client.player?.sendOverlayMessage(text.value)
        }
        override fun sendCommand(command: String) {
            client.player?.connection?.sendCommand(
                command.removePrefix("/")
            )
        }
    }
}