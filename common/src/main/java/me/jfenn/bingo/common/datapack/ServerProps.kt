package me.jfenn.bingo.common.datapack

import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.readText

object ServerProps {

    private val gameDir = FabricLoader.getInstance().gameDir

    private val serverProperties = gameDir.resolve("server.properties")

    private fun readServerProperties() = try {
        serverProperties.readText().split(Regex("\\R"))
    } catch (e: Throwable) {
        null
    }

    // If server.properties exists, find level-name=, otherwise default to "world"
    val levelName = readServerProperties()
        ?.find { it.startsWith("level-name=") }
        ?.removePrefix("level-name=")
        ?: "world"
}