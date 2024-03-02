package me.jfenn.bingo.impl

import me.jfenn.bingo.api.ITextSerializer
import net.minecraft.text.Text

class TextSerializer : ITextSerializer {
    override fun toJson(text: Text): String {
        return Text.Serializer.toJson(text)
    }

    override fun fromJson(json: String): Text {
        return Text.Serializer.fromJson(json)!!
    }
}
