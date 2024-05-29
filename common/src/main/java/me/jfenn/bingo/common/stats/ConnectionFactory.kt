package me.jfenn.bingo.common.stats

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.HikariDataSource
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.stats.sql.StatsDatabase
import net.fabricmc.loader.api.FabricLoader
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import kotlin.io.path.absolutePathString
import kotlin.time.measureTime

class ConnectionFactory(
    private val log: Logger,
    private val config: BingoConfig,
) {

    private val defaultUrl = FabricLoader.getInstance()
        .configDir
        .absolutePathString()
        .let { configDir ->
            "jdbc:sqlite:$configDir/$MOD_ID/stats.db"
        }

    private val url = config.databaseUrl ?: defaultUrl

    private val dataSource = HikariDataSource().apply {
        jdbcUrl = url
        username = config.databaseUser ?: ""
        password = config.databasePass ?: ""
    }

    fun create(): StatsDatabase {
        log.info("Starting Flyway migrations...")

        measureTime {
            // configure Flyway and run migrations - these are just sql files saved under `src/sqldelight/.../V1_init.sqm`
            val flyway = Flyway.configure()
                .locations("$MOD_ID_BINGO/migrations")
                .dataSource(dataSource)
                .load()

            flyway.migrate()
        }.also {
            log.info("Done! ($it)")
        }

        return StatsDatabase(dataSource.asJdbcDriver())
    }

    fun close() {
        log.info("[ConnectionFactory] Closing stats data source...")
        dataSource.close()
    }

}