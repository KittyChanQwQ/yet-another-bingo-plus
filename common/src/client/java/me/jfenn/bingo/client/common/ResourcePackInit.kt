package me.jfenn.bingo.client.common

import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import org.slf4j.Logger

class ResourcePackInit(
    log: Logger,
) {
    companion object {
        private val classic = ResourceManagerHelper.registerBuiltinResourcePack(
            Identifier("bingo", "classic"),
            FabricLoader.getInstance().getModContainer("yet-another-minecraft-bingo-common").orElseThrow(),
            ResourcePackActivationType.NORMAL
        )

        private val futurist = ResourceManagerHelper.registerBuiltinResourcePack(
            Identifier("bingo", "futurist"),
            FabricLoader.getInstance().getModContainer("yet-another-minecraft-bingo-common").orElseThrow(),
            ResourcePackActivationType.NORMAL
        )
    }

    init {
        if (!classic) log.error("Classic resource pack could not be registered!")
        if (!futurist) log.error("Futurist resource pack could not be registered!")
    }
}