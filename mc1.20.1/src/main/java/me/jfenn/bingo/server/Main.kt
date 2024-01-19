package me.jfenn.bingo.server

import me.jfenn.bingo.common.BingoKoin
import me.jfenn.bingo.common.commonModule
import me.jfenn.bingo.sharedModule
import net.fabricmc.api.DedicatedServerModInitializer
import org.koin.core.logger.Level
import org.koin.dsl.koinApplication
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory

object Main : DedicatedServerModInitializer {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun onInitializeServer() {
        log.info("Starting Bingo Server koin application")
        koinApplication {
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(sharedModule, commonModule)
        }.also {
            BingoKoin.koinApp = it
        }
    }
}