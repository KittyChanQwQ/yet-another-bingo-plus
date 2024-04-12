package me.jfenn.bingo.impl

import me.jfenn.bingo.api.ITextSerializer
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text

class TextSerializer(
    private val server: MinecraftServer,
) : ITextSerializer {

    override fun toJson(text: Text): String {
        return Text.Serialization.toJsonString(text, server.registryManager)
    }

    override fun fromJson(json: String): Text {
        return Text.Serialization.fromJson(json, server.registryManager)!!
    }

}
