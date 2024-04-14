package me.jfenn.bingo.client.impl

import me.jfenn.bingo.api.ITextSerializer
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.slf4j.Logger

class ClientTextSerializerImpl(
    private val log: Logger,
) : ITextSerializer {
    override fun toJson(text: Text): String {
        val registries = MinecraftClient.getInstance().world?.registryManager
            ?: run {
                log.error("[ClientTextSerializerImpl] toJson - unable to get world registryManager")
                return "{}"
            }

        return Text.Serialization.toJsonString(text, registries)
    }

    override fun fromJson(json: String): Text {
        val registries = MinecraftClient.getInstance().world?.registryManager
            ?: run {
                log.error("[ClientTextSerializerImpl] fromJson - unable to get world registryManager")
                return Text.empty()
            }

        return Text.Serialization.fromJson(json, registries) ?: Text.empty()
    }
}