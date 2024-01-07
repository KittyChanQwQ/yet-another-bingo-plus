package me.jfenn.bingo.client

import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.client.impl.ButtonService
import me.jfenn.bingo.commonModule
import net.fabricmc.api.ClientModInitializer
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory

object Main : ClientModInitializer {

    private val log = LoggerFactory.getLogger(this::class.java)

    val clientModule = module {
        singleOf(::ButtonService) bind IButtonService::class
    }

    init {
        log.info("Starting Bingo Client koin application")
        startKoin {
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(commonModule)
            modules(clientModule)
        }
    }

    override fun onInitializeClient() {

    }
}
