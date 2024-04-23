package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.MOD_ID
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class MigrationHandler(
    private val log: Logger,
    private val configService: ConfigService,
    private val config: BingoConfig,
) {

    private val migrations = mutableMapOf<Int, (BingoConfig) -> Unit>()

    init {
        val loader = FabricLoader.getInstance()

        migrations[1] = {
            if (loader.environmentType == EnvType.SERVER) {
                loader.gameDir.resolve("world/datapacks/bingo-lobby.zip")
                    .takeIfMd5("89c7644b5cbcf785f25839364ad61788")
                    ?.deleteIfExists()

                loader.gameDir.resolve("world/datapacks/bingo-lobby.zip")
                    .takeIfMd5("df4016673876474b77cac543ebb3f7f7")
                    ?.deleteIfExists()
            }

            loader.configDir.resolve("bingo/players.json")
                .deleteIfExists()

            loader.configDir.resolve("bingo/overworld.tierlist.json")
                .takeIfMd5("16408324182d09a1a3a80317c0c2269b")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/overworld.tierlist.json")
                .takeIfMd5("45ca6dac60d9cf5232a3c7b21610bc99")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/overworld.tierlist.json")
                .takeIfMd5("9fbe1eb9ed3e4744ec7272e13cb7e7fd")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/nether.tierlist.json")
                .takeIfMd5("7eb68b14b0288982b86a3c6b856482e3")
                ?.deleteIfExists()
        }

        migrations[2] = {
            loader.configDir.resolve("bingo/all_items.tierlist.json")
                .takeIfMd5("37946b44952cab04cd2dde840da5beac")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/overworld-only.tierlist.json")
                .takeIfMd5("747b65ffd7b923cc4e2f746dffacd4e0")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/simplified.tierlist.json")
                .takeIfMd5("707151ae1614dc508ad264677be397e2")
                ?.deleteIfExists()

            Config.readStream("all_items.tierlist.json").close()
            Config.readStream("nether-only.tierlist.json").close()
            Config.readStream("overworld-only.tierlist.json").close()
            Config.readStream("simplified.tierlist.json").close()
        }

        migrations[3] = {
            Config.readStream("advancements.tierlist.json").close()
        }

        migrations[4] = {
            loader.configDir.resolve("bingo/all_items.tierlist.json")
                .takeIfMd5("85984ea87c88d93cc9069d0cef529338")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/overworld-only.tierlist.json")
                .takeIfMd5("08e90ebc0fc941be90cdfd3f57035983")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/simplified.tierlist.json")
                .takeIfMd5("1c256401902baeb115b9a7d1547fc417")
                ?.deleteIfExists()

            loader.configDir.resolve("bingo/advancements.tierlist.json")
                .takeIfMd5("000ba600d573dc29e3c12be51ce52ea9")
                ?.deleteIfExists()

            Config.readStream("all_items.tierlist.json").close()
            Config.readStream("overworld-only.tierlist.json").close()
            Config.readStream("simplified.tierlist.json").close()
            Config.readStream("advancements.tierlist.json").close()
        }
    }

    private fun Path.takeIfMd5(md5Str: String): Path? {
        if (!this.isRegularFile()) return null

        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(Files.readAllBytes(this))
        val hexDigest = BigInteger(1, digest).toString(16)

        val matches = hexDigest == md5Str
        if (matches) {
            log.info("MATCH $hexDigest -- $pathString")
        } else {
            log.info("FAIL $hexDigest -- $pathString")
        }

        return takeIf { matches }
    }

    fun runMigrations() {
        val fromVersion = config.version
        val maxVersion = migrations.keys.max()

        for (i in fromVersion+1..maxVersion) {
            log.info("$MOD_ID: running migrations to v$i")
            migrations[i]?.invoke(config)
        }

        configService.config = config.copy(version = maxVersion)
        configService.options = configService.options.copy()
        configService.optionsDefault = configService.optionsDefault.copy()
        configService.playerConfig = configService.playerConfig.copy()
    }

}