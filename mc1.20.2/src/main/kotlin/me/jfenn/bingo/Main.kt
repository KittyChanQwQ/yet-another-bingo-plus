package me.jfenn.bingo

import me.jfenn.bingo.api.*
import me.jfenn.bingo.impl.*
import net.fabricmc.api.ModInitializer
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger

object Main : ModInitializer {

    val mainModule = module {
        singleOf(::AdvancementManager) bind IAdvancementManager::class
        singleOf(::PersistentStateManager) bind IPersistentStateManager::class
        singleOf(::ScoreboardManager) bind IScoreboardManager::class
        singleOf(::StructureManager) bind IStructureManager::class
        singleOf(::TeamManager) bind ITeamManager::class
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