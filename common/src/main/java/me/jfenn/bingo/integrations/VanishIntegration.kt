package me.jfenn.bingo.integrations

import me.drex.vanish.api.VanishAPI
import me.jfenn.bingo.common.config.BingoConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.Logger

interface VanishIntegration {

    fun isInstalled() = VanishIntegration.isInstalled()

    fun setVanish(player: ServerPlayerEntity, isVanished: Boolean)

    companion object {

        fun isInstalled() = FabricLoader.getInstance().isModLoaded("melius-vanish")

        fun create(
            log: Logger,
            config: BingoConfig,
        ): VanishIntegration {
            return if (isInstalled() && config.integrations.vanish) {
                log.info("Integrations: Vanish is installed!")
                VanishIntegrationImpl()
            } else {
                log.info("Integrations: Vanish is not installed - https://modrinth.com/mod/vanish (optional, for better spectator)")
                VanishIntegrationDummy
            }
        }
    }
}

object VanishIntegrationDummy : VanishIntegration {
    override fun setVanish(player: ServerPlayerEntity, isVanished: Boolean) {}
}

class VanishIntegrationImpl : VanishIntegration {
    override fun setVanish(player: ServerPlayerEntity, isVanished: Boolean) {
        VanishAPI.setVanish(player, isVanished)
    }
}
