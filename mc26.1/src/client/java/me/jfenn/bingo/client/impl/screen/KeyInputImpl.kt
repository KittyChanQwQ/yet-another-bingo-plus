package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.platform.screen.IKeyInput
import net.minecraft.client.input.KeyEvent

class KeyInputImpl(val input: KeyEvent) : IKeyInput {
    override val isEscape: Boolean
        get() = input.isEscape
}
