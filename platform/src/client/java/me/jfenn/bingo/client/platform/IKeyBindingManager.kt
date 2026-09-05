package me.jfenn.bingo.client.platform

interface IKeyBindingManager {
    fun registerKey(
        translationKey: String,
        code: KeyCode,
        category: String,
    ): IKeyBinding
}

enum class KeyCode {
    KEY_Y,
    UNKNOWN,
}

interface IKeyBinding {
    fun isPressed(): Boolean
    fun wasPressed(): Boolean
}
