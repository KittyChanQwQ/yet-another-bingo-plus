package me.jfenn.bingo.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import me.jfenn.bingo.api.ITextSerializer
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs

class TextSerializer : ITextSerializer {

    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    override fun toJson(text: Text): String {
        // this achieves Text.Serialization.toJsonString, but statically, without requiring RegistryWrapper
        val element = TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow()
        return gson.toJson(element)
    }

    override fun fromJson(json: String): Text {
        // this achieves Text.Serialization.fromJson, but statically, without requiring RegistryWrapper
        val element = JsonParser.parseString(json)
        return TextCodecs.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow()
    }

}
