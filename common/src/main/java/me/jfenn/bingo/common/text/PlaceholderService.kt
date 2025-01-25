package me.jfenn.bingo.common.text

import me.jfenn.bingo.api.services.TextPlaceholders
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text

class PlaceholderService(
    private val server: MinecraftServer,
) {
    fun parseText(message: Text): Text {
        var ret = message
        for (service in TextPlaceholders.services)
            ret = service.parseText(ret, server)

        return ret
    }
}