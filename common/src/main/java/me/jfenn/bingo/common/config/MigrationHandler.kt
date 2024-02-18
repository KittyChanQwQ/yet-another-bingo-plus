package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.scope.BingoKoin
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

object MigrationHandler {

    private val log by BingoKoin.koinApp.koin.inject<Logger>()
    private val migrations = mutableMapOf<Int, (BingoConfig) -> Unit>()

    init {
        val loader = FabricLoader.getInstance()

        migrations[1] = {
            loader.gameDir.resolve("world/datapacks/bingo-lobby.zip")
                .takeIfMd5("df4016673876474b77cac543ebb3f7f7")
                ?.deleteIfExists()

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

    fun migrate(fromVersion: Int, config: BingoConfig): Int {
        val maxVersion = migrations.keys.max()
        for (i in fromVersion+1..maxVersion) {
            log.info("$MOD_ID: running migrations to v$i")
            migrations[i]?.invoke(config)
        }
        return maxVersion
    }

}