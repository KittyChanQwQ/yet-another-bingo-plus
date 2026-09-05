package me.jfenn.bingo.client.impl

import com.mojang.blaze3d.platform.InputConstants
import me.jfenn.bingo.client.platform.IKeyBinding
import me.jfenn.bingo.client.platform.IKeyBindingManager
import me.jfenn.bingo.client.platform.KeyCode
import me.jfenn.bingo.common.MOD_ID_BINGO
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

class KeyBindingManager : IKeyBindingManager {
    private val categoryInstance by lazy {
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID_BINGO, "bingo"))
    }

    private fun KeyCode.toKeyCode(): Int = when (this) {
        KeyCode.KEY_Y -> GLFW.GLFW_KEY_Y
        KeyCode.UNKNOWN -> GLFW.GLFW_KEY_UNKNOWN
    }

    override fun registerKey(translationKey: String, code: KeyCode, category: String): IKeyBinding {
        return KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                code.toKeyCode(),
                categoryInstance,
            )
        ).let { KeyBindingImpl(it) }
    }

    class KeyBindingImpl(
        private val binding: KeyMapping,
    ) : IKeyBinding {
        override fun isPressed(): Boolean = binding.isDown
        override fun wasPressed(): Boolean = binding.consumeClick()
    }
}