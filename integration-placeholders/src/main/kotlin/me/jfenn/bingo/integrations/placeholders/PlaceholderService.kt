package me.jfenn.bingo.integrations.placeholders

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import me.jfenn.bingo.api.services.TextPlaceholderService
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text

class PlaceholderService : TextPlaceholderService {
    override fun parseText(message: Text, server: MinecraftServer): Text {
        return Placeholders.parseText(message, PlaceholderContext.of(server))
    }
}