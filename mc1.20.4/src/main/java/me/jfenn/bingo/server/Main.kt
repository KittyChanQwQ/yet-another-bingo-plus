package me.jfenn.bingo.server

import me.jfenn.bingo.commonModule
import net.fabricmc.api.DedicatedServerModInitializer
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory

object Main : DedicatedServerModInitializer {

    private val log = LoggerFactory.getLogger(this::class.java)

    init {
        log.info("Starting Bingo Server koin application")
        startKoin {
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(commonModule)
        }
    }

    override fun onInitializeServer() {

    }
}
