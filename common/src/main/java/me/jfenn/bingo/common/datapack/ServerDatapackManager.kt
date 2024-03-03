package me.jfenn.bingo.common.datapack

import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.scope.BingoComponent
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger

class ServerDatapackManager(
    log: Logger,
) : BingoComponent() {

    init {
        // If the env is on a dedicated server, we need to install the datapack manually
        val loader = FabricLoader.getInstance()
        if (loader.environmentType == EnvType.SERVER) {
            val datapackPath = loader.gameDir.resolve("world/datapacks/$MOD_ID.zip")

            log.info("Installing the $MOD_ID datapack in $datapackPath")
            DatapackService.copyDataPack(datapackPath)
        }
    }

}