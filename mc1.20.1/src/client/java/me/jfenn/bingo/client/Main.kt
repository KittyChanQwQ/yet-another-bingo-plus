package me.jfenn.bingo.client

import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.client.common.commonClientModule
import me.jfenn.bingo.client.impl.ButtonService
import me.jfenn.bingo.client.impl.DrawService
import me.jfenn.bingo.common.commonInit
import me.jfenn.bingo.common.commonModule
import me.jfenn.bingo.common.config.MigrationHandler
import me.jfenn.bingo.common.scope.BingoKoin
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
        single { DrawService } bind IDrawServiceFactory::class
    }

    override fun onInitializeClient() {
        log.info("Starting Bingo migration runner...")

        koinApplication {
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(sharedModule, commonModule)
        }.also {
            it.koin.get<MigrationHandler>().runMigrations()
            it.close()
        }

        log.info("Starting Bingo Client application...")

        koinApplication {
            BingoKoin.koinApp = this@koinApplication
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(sharedModule, commonModule, clientModule, commonClientModule)
        }.also {
            it.koin.commonInit()
        }
    }
}
