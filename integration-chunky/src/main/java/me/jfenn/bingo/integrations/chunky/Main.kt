package me.jfenn.bingo.integrations.chunky

import me.jfenn.bingo.api.BingoEvents
import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.config.IConfigManager
import me.jfenn.bingo.platform.scope.BingoKoin
import net.fabricmc.api.ModInitializer

object Main : ModInitializer {
    private const val CONFIG_PATH = "yet-another-minecraft-bingo/chunky.json"
    private var chunkyController: ChunkyController? = null

    override fun onInitialize() {
        BingoEvents.INIT.register {
            val scope = BingoKoin.getScope(it.server)!!
            val environment = scope.get<IModEnvironment>()
            val configManager = scope.get<IConfigManager>()

            if (environment.isModLoaded("chunky")) {
                val config = try {
                    configManager.read(ChunkyConfig::class, CONFIG_PATH)
                } catch (e: Throwable) {
                    ChunkyConfig()
                }
                configManager.write(ChunkyConfig::class, CONFIG_PATH, config)

                if (config.enabled) {
                    chunkyController = ChunkyController(
                        api = it,
                        server = scope.get(),
                        config = config,
                        commandRunner = scope.get(),
                        log = scope.get(),
                    )
                }
            }
        }

        BingoEvents.CLOSE.register {
            chunkyController?.close()
            chunkyController = null
        }
    }
}
