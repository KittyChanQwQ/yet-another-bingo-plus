package me.jfenn.bingo.common.utils.text

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TextColor {
    @SerialName("black") BLACK,
    @SerialName("dark_blue") DARK_BLUE,
    @SerialName("dark_green") DARK_GREEN,
    @SerialName("dark_aqua") DARK_AQUA,
    @SerialName("dark_red") DARK_RED,
    @SerialName("dark_purple") DARK_PURPLE,
    @SerialName("gold") GOLD,
    @SerialName("gray") GRAY,
    @SerialName("dark_gray") DARK_GRAY,
    @SerialName("blue") BLUE,
    @SerialName("green") GREEN,
    @SerialName("aqua") AQUA,
    @SerialName("red") RED,
    @SerialName("light_purple") LIGHT_PURPLE,
    @SerialName("yellow") YELLOW,
    @SerialName("white") WHITE,
}