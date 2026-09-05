package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.platform.screen.IKeyInput
import net.minecraft.client.input.KeyEvent

class KeyInputImpl(val input: KeyEvent) : IKeyInput {
    override val isArrowLeft: Boolean
        get() = input.isLeft
    override val isArrowRight: Boolean
        get() = input.isRight
    override val isEscape: Boolean
        get() = input.isEscape
}
