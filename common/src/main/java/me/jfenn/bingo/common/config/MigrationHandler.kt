package me.jfenn.bingo.common.config

import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.data.TierListLoader
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.path.*

class MigrationHandler(
    private val log: Logger,
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
        }

        migrations[3] = {}

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
        }

        migrations[5] = {
            // move the "config/bingo" dir to "config/yet-another-minecraft-bingo"
            val originalDir = loader.configDir.resolve(MOD_ID)
            val newDir = loader.configDir.resolve(MOD_ID_BINGO)
            if (originalDir.resolve("game-options.json").toFile().exists()) {
                log.info("[MigrationHandler] Renaming config/$MOD_ID -> config/$MOD_ID_BINGO")
                newDir.toFile().deleteRecursively()
                Files.move(originalDir, newDir, StandardCopyOption.REPLACE_EXISTING)
            } else {
                log.warn("[MigrationHandler] Not renaming config/$MOD_ID as it does not exist.")
            }
        }

        migrations[6] = {
            loader.configDir.resolve("$MOD_ID_BINGO/all_items.tierlist.json")
                .takeIfMd5("4e1fbf43d0995bc3b4c85b985c9b6c1f")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/simplified.tierlist.json")
                .takeIfMd5("0135d16de992bdfea1d7c79d6c5b7054")
                ?.deleteIfExists()
        }

        migrations[7] = { // 2.1.0-alpha.0
            loader.configDir.resolve("$MOD_ID_BINGO/nether-only.tierlist.json")
                .takeIfMd5("7eb68b14b0288982b86a3c6b856482e3")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/overworld-only.tierlist.json")
                .takeIfMd5("7f79d26e384946de62c7d9d4f54105e2")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/advancements.tierlist.json")
                .takeIfMd5("19ea980bff9cd124ece633189d2257a2")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/all_items.tierlist.json")
                .takeIfMd5("cfb3aeebd054d727e52b4398aa6c0f8b")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/simplified.tierlist.json")
                .takeIfMd5("0d4f8a30e26cfcbc44a9096d6eef556e", "d4f8a30e26cfcbc44a9096d6eef556e")
                ?.deleteIfExists()
        }

        migrations[11] = { // 2.1.0-alpha.3
            loader.configDir.resolve("$MOD_ID_BINGO/advancements.tierlist.json")
                .takeIfMd5("99c0b10329da4ff1c5224d9f86a3cb2a")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/challenge.tierlist.json")
                .takeIfMd5("dd036cdc11bb671f16ce48fdef978d7b", "7a5449d0c4d6812200c0762090666794")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/all_items.tierlist.json")
                .takeIfMd5("b20e4626cbf9115ac6170cc39f2a109a")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/simplified.tierlist.json")
                .takeIfMd5("2ea9f498a9f5203392d1eb8244136c6f")
                ?.deleteIfExists()
        }

        migrations[16] = { // 2.2.1
            loader.configDir.resolve("$MOD_ID_BINGO/all_items.tierlist.json")
                .takeIfMd5("8af987dce4f22bdd948dab0be442491e", "80db1d320d67b16046eff5d16a3b911a", "7bb179acf2c50992091a50eda4e115b2")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/simplified.tierlist.json")
                .takeIfMd5("8e53697667ad30847642932edfd302d7", "704f1e91c43487a4495bb1d7ac51eeb1")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/challenge.tierlist.json")
                .takeIfMd5("9c0c3347056b79c6dafef1dcb2316f69", "762fb665525a9a7408e460184f1e5803", "980848e7fc9bdc54030a449a65548997", "3509f527976c42b4f72b5d9f5ae57bac")
                ?.deleteIfExists()
        }

        migrations[18] = { // 2.2.5
            loader.configDir.resolve("$MOD_ID_BINGO/challenge.tierlist.json")
                .takeIfMd5("3509f527976c42b4f72b5d9f5ae57bac", "efe9f31c33a51e893c4aff44ffb8f51e")
                ?.deleteIfExists()
        }

        migrations[22] = { // 2.3.0
            loader.configDir.resolve("$MOD_ID_BINGO/all_items.tierlist.json")
                .takeIfMd5("73eddb865b11831a711b9c58753f2bfb")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/advancements.tierlist.json")
                .takeIfMd5("30a50846828cd3e432ff67a5ded29c79", "4b95c5d16e8ee6f5354ddaa872c520c5")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/challenge.tierlist.json")
                .takeIfMd5("79a1242373e76d5bafd9a8843f8130a1", "f13fe7bf41603f2d4760fbc64d00eca8")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/items.tierlist.json")
                .takeIfMd5("9336c61e3bc04ab0d5551558f6728c1c")
                ?.deleteIfExists()

            loader.configDir.resolve("$MOD_ID_BINGO/simplified.tierlist.json")
                .takeIfMd5("f692573a7c0437c3f7bb8e35e034a873")
                ?.deleteIfExists()
        }
    }

    private fun md5Of(path: Path): String? {
        val md = MessageDigest.getInstance("MD5")
        val digest = try {
            md.digest(Files.readAllBytes(path))
        } catch (e: IOException) {
            log.error("[MigrationHandler] Error reading $path")
            return null
        }
        val hexDigest = BigInteger(1, digest).toString(16)
        return hexDigest
    }

    private fun Path.takeIfMd5(vararg md5Str: String): Path? {
        if (!this.isRegularFile()) return null

        val hexDigest = md5Of(this) ?: return null
        val matches = md5Str.contains(hexDigest)
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

        val configService = ConfigService()
        configService.config = configService.config.copy(version = maxVersion)
        configService.options = configService.options.copy()
        configService.optionsDefault = configService.optionsDefault.copy()
        configService.playerConfig = configService.playerConfig.copy()

        // - tierlist files should be re-generated on each launch if removed:
        for (list in TierListLoader.BUILTIN_TIER_LISTS) {
            val filePath = Config.configDir.resolve("$MOD_ID_BINGO/$list${TierListLoader.FILE_SUFFIX}")
            if (!Files.exists(filePath)) {
                FileOutputStream(filePath.toFile()).use { output ->
                    javaClass.getResourceAsStream("/data/$MOD_ID_BINGO/$MOD_ID_BINGO/tierlists/$list${TierListLoader.FILE_SUFFIX}")!!
                        .use { input -> IOUtils.copy(input, output) }
                }
            }
        }

        if (log.isDebugEnabled) {
            Config.configDir.resolve(MOD_ID_BINGO)
                .takeIf { it.isDirectory() }
                ?.listDirectoryEntries(glob = "*.json")
                ?.forEach {
                    log.info("| $MOD_ID_BINGO | ${it.fileName.toString().padEnd(40)} | ${md5Of(it)} |")
                }
        }
    }

}