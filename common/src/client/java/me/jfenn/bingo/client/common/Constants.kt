package me.jfenn.bingo.client.common

import me.jfenn.bingo.client.platform.screen.IButtonFactory.ButtonTextures
import me.jfenn.bingo.platform.Identifier

val DECORATION_ADVANCEMENT_ID = Identifier("minecraft:bingo/image_frame_advancement")
val DECORATION_FORBIDDEN_ID = Identifier("minecraft:bingo/image_frame_forbidden")
val DECORATION_FREE_SPACE_ID = Identifier("minecraft:bingo/image_frame_free_space")
val DECORATION_LOCKED_ID = Identifier("minecraft:bingo/image_frame_locked")

val BUTTON_GAME_CONTINUE = ButtonTextures(
    texture = Identifier("minecraft:bingo/button_game_continue"),
    focusedTexture = Identifier("minecraft:bingo/button_game_continue_focused"),
    inactiveTexture = Identifier("minecraft:bingo/button_game_continue_inactive")
)

val BUTTON_GAME_GENERIC = ButtonTextures(
    texture = Identifier("minecraft:bingo/button_game_generic"),
    focusedTexture = Identifier("minecraft:bingo/button_game_generic_focused"),
    inactiveTexture = Identifier("minecraft:bingo/button_game_generic_inactive")
)

val BUTTON_BINGO = ButtonTextures(
    texture = Identifier("minecraft:bingo/button"),
    focusedTexture = Identifier("minecraft:bingo/button_focused")
)
