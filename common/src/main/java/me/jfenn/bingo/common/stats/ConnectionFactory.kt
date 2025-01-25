package me.jfenn.bingo.common.stats

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.HikariDataSource
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.platform.ICloseEvent
import me.jfenn.bingo.sql.BingoDatabase
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.io.FileUtils
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.absolutePathString

class ConnectionFactory(
    private val log: Logger,
    private val config: BingoConfig,
    closeEvent: ICloseEvent,
) {

    private val defaultUrl = FabricLoader.getInstance()
        .configDir
        .absolutePathString()
        .let { configDir ->
            "jdbc:sqlite:$configDir/$MOD_ID_BINGO/bingo.db"
        }

    private val url = config.databaseUrl ?: defaultUrl

    private val dataSource = HikariDataSource().apply {
        jdbcUrl = url
        username = config.databaseUser ?: ""
        password = config.databasePass ?: ""
    }

    private fun extractMigrations(tmpDir: Path) {
        this::class.java.getResourceAsStream("/$MOD_ID_BINGO/migrations.zip")?.use { stream ->
            ZipInputStream(stream).use { zipStream ->
                while (true) {
                    val entry = zipStream.nextEntry ?: break
                    if (entry.isDirectory) continue

                    val outPath = tmpDir.resolve(entry.name)
                    log.debug("Copying file -> {}", outPath)
                    try {
                        Files.copy(zipStream, outPath)
                    } catch (e: IOException) {
                        log.error("[ConnectionFactory] Error copying lobby file ${entry.name}", e)
                    }

                    zipStream.closeEntry()
                }
            }
        } ?: run {
            log.error("[ConnectionFactory] Unable to find migrations.zip in resources")
        }
    }

    fun create(): BingoDatabase {
        log.measureTime("Running Flyway migrations...") {
            val tmpDir = Files.createTempDirectory("bingo-migrations")
            extractMigrations(tmpDir)

            // configure Flyway and run migrations - these are just sql files saved under `src/sqldelight/.../V1_init.sqm`
            val flyway = Flyway.configure()
                // need to use filesystem: locations here, as Quilt does some wacky things to the classloader
                // ...so I'm extracting these to a tmp dir and telling Flyway to look there instead
                .locations("filesystem:${tmpDir.absolutePathString()}")
                .failOnMissingLocations(true)
                .dataSource(dataSource)
                .load()

            flyway.migrate()

            FileUtils.deleteDirectory(tmpDir.toFile())
        }

        return BingoDatabase(dataSource.asJdbcDriver())
    }

    fun close() {
        log.info("[ConnectionFactory] Closing stats data source...")
        dataSource.close()
    }

    init {
        closeEvent.onClose { close() }
    }

}