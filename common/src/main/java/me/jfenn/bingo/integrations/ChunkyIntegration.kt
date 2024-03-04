package me.jfenn.bingo.integrations

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.utils.runSilentCommand
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger

interface ChunkyIntegration {

    fun startPregen(server: MinecraftServer)

    fun cancelTasks(server: MinecraftServer)

    companion object {
        fun isInstalled() = FabricLoader.getInstance().isModLoaded("chunky")

        fun create(
            log: Logger,
            config: BingoConfig,
        ): ChunkyIntegration {
            return if (isInstalled() && config.integrations.chunky) {
                log.info("Integrations: Chunky is installed!")
                ChunkyIntegrationImpl(log, config)
            } else {
                log.info("Integrations: Chunky is not installed - https://modrinth.com/plugin/chunky (optional, for world pre-generation)")
                ChunkyIntegrationDummy
            }
        }
    }
}

object ChunkyIntegrationDummy : ChunkyIntegration {
    override fun startPregen(server: MinecraftServer) = Unit
    override fun cancelTasks(server: MinecraftServer) = Unit
}

class ChunkyIntegrationImpl(
    private val log: Logger,
    private val config: BingoConfig,
) : ChunkyIntegration {
    override fun startPregen(server: MinecraftServer) {
        try {
            config.integrations.chunkyWorlds.forEach { world, size ->
                server.runSilentCommand("chunky world $world")
                server.runSilentCommand("chunky radius $size")
                server.runSilentCommand("chunky start")
            }
        } catch (e: Throwable) {
            log.error("Error starting chunky pre-generation:", e)
        }
    }

    override fun cancelTasks(server: MinecraftServer) {
        try {
            server.runSilentCommand("chunky cancel")
            server.runSilentCommand("chunky confirm")
        } catch (e: Throwable) {
            log.error("Error cancelling chunky pre-generation:", e)
        }
    }
}
