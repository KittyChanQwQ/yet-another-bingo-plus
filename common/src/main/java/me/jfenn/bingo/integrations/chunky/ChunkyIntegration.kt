package me.jfenn.bingo.integrations.chunky

import me.jfenn.bingo.api.ICommandRunner
import me.jfenn.bingo.common.config.BingoConfig
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
            commandRunner: ICommandRunner,
        ): ChunkyIntegration {
            return if (isInstalled() && config.integrations.chunky) {
                log.info("Integrations: Chunky is installed!")
                ChunkyIntegrationImpl(log, config, commandRunner)
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
    private val commandRunner: ICommandRunner,
) : ChunkyIntegration {
    override fun startPregen(server: MinecraftServer) {
        try {
            config.integrations.chunkyWorlds.forEach { (world, size) ->
                if (size > 0) {
                    commandRunner.runSilentCommand(server, "chunky world $world")
                    commandRunner.runSilentCommand(server, "chunky radius $size")
                    commandRunner.runSilentCommand(server, "chunky start")
                }
            }
            commandRunner.runSilentCommand(server, "chunky continue")
        } catch (e: Throwable) {
            log.error("Error starting chunky pre-generation:", e)
        }
    }

    override fun cancelTasks(server: MinecraftServer) {
        try {
            commandRunner.runSilentCommand(server, "chunky cancel")
            commandRunner.runSilentCommand(server, "chunky confirm")
        } catch (e: Throwable) {
            log.error("Error cancelling chunky pre-generation:", e)
        }
    }
}
