package me.jfenn.bingo.common.datapack

import me.jfenn.bingo.common.BINGO
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.platform.IModEnvironment
import org.slf4j.Logger
import kotlin.io.path.deleteIfExists

class ServerDatapackManager(
    log: Logger,
    config: BingoConfig,
    environment: IModEnvironment,
    serverProps: ServerProps,
    lobbyConfigService: LobbyConfigService,
) : BingoComponent() {

    init {
        // If the env is on a dedicated server, we need to install the datapack manually
        if (environment.envType == IModEnvironment.EnvType.SERVER) {
            val datapackPath = environment.gameDir.resolve("${serverProps.levelName}/datapacks/$BINGO.zip")
            if (config.server.isLobbyMode) {
                log.info("[ServerDatapackManager] isLobbyMode=true - Installing the $BINGO datapack in $datapackPath")
                lobbyConfigService.copyDataPack(datapackPath)
            } else {
                log.info("[ServerDatapackManager] isLobbyMode=false - Verifying that $datapackPath does not exist...")
                if (datapackPath.deleteIfExists()) {
                    log.warn("[ServerDatapackManager] Deleted $datapackPath")
                }
            }
        }
    }

}