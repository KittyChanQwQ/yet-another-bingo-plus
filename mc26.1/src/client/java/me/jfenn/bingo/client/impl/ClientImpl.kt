package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.impl.draw.FontImpl
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.IClientPlayer
import me.jfenn.bingo.client.platform.renderer.IFont
import me.jfenn.bingo.platform.BlockHitResult
import me.jfenn.bingo.platform.EntityHitResult
import me.jfenn.bingo.platform.HitResult
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.text.IText
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.joml.Vector2i
import java.util.concurrent.Executor
import net.minecraft.world.phys.BlockHitResult as MinecraftBlockHitResult
import net.minecraft.world.phys.EntityHitResult as MinecraftEntityHitResult

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
                mouse.getScaledXPos(client.window).toInt(),
                mouse.getScaledYPos(client.window).toInt(),
            )
        }

    override var screen: Screen?
        get() = client.screen
        set(value) { client.setScreen(value) }

    override fun lookingAt(): HitResult? {
        return when (val hitResult = client.hitResult) {
            is MinecraftEntityHitResult -> EntityHitResult(hitResult.entity.uuid)
            is MinecraftBlockHitResult -> BlockHitResult(BlockPosition(hitResult.blockPos.x, hitResult.blockPos.y, hitResult.blockPos.z))
            else -> null
        }
    }

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