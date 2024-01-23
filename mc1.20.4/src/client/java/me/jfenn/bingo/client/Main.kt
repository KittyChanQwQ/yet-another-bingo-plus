package me.jfenn.bingo.client

import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.client.common.commonClientModule
import me.jfenn.bingo.client.impl.ButtonService
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.commonModule
import me.jfenn.bingo.sharedModule
import net.fabricmc.api.ClientModInitializer
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory

object Main : ClientModInitializer {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val clientModule = module {
        singleOf(::ButtonService) bind IButtonService::class
    }

    override fun onInitializeClient() {
        log.info("Starting Bingo Client koin application")
        koinApplication {
            BingoKoin.koinApp = this@koinApplication
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(sharedModule, commonModule, clientModule, commonClientModule)
        }
    }
}