package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.text.IText
import net.minecraft.network.chat.Component

interface ITextSerializer {
    fun toJson(text: IText): String
    fun fromJson(json: String): Component
    fun toRawString(text: IText): String
}