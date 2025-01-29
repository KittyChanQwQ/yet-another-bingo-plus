package me.jfenn.bingo.client.common

import me.jfenn.bingo.common.MOD_ID_BINGO
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import org.slf4j.Logger

internal class ResourcePackInit(
    log: Logger,
) {
    companion object {
        private val classic = ResourceManagerHelper.registerBuiltinResourcePack(
            Identifier.of("bingo", "classic"),
            FabricLoader.getInstance().getModContainer(MOD_ID_BINGO).orElseThrow(),
            ResourcePackActivationType.NORMAL
        )

        private val futurist = ResourceManagerHelper.registerBuiltinResourcePack(
            Identifier.of("bingo", "futurist"),
            FabricLoader.getInstance().getModContainer(MOD_ID_BINGO).orElseThrow(),
            ResourcePackActivationType.NORMAL
        )
    }

    init {
        if (!classic) log.error("Classic resource pack could not be registered!")
        if (!futurist) log.error("Futurist resource pack could not be registered!")
    }
}