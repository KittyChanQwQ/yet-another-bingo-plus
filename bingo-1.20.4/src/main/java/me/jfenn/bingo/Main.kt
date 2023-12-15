package me.jfenn.bingo

import me.jfenn.bingo.api.IScoreboardManager
import net.fabricmc.api.ModInitializer
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import me.jfenn.bingo.impl.ScoreboardManager
import org.koin.dsl.bind

object Main : ModInitializer {

    val mainModule = module {
        singleOf(::ScoreboardManager) bind IScoreboardManager::class
    }

    init {
        startKoin {
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(mainModule)
        }
    }

    override fun onInitialize() {

    }
}