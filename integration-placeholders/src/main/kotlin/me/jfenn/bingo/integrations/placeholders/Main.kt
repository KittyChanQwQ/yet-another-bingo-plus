package me.jfenn.bingo.integrations.placeholders

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

object Main : ModInitializer {
    override fun onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
            PlaceholdersIntegration().registerPlaceholders()
        }
    }
}
