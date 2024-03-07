package me.jfenn.bingo.client.common

import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.PACKET_CARD_OPEN
import me.jfenn.bingo.common.scope.BingoComponent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

class BingoOpenCardController : BingoComponent() {

    private val openCardKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.$MOD_ID.open_card",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.$MOD_ID",
        )
    )

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (it.isPaused) return@register

            if (openCardKey.wasPressed() && ClientPlayNetworking.canSend(PACKET_CARD_OPEN)) {
                ClientPlayNetworking.send(PACKET_CARD_OPEN, PacketByteBufs.create())
            }
        }
    }
}