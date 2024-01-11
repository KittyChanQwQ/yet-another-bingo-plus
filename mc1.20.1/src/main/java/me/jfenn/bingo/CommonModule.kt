package me.jfenn.bingo

import me.jfenn.bingo.api.*
import me.jfenn.bingo.impl.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val commonModule = module {
    singleOf(::AdvancementManager) bind IAdvancementManager::class
    singleOf(::OfflinePlayerAccessor) bind IOfflinePlayerAccessor::class
    singleOf(::PersistentStateManager) bind IPersistentStateManager::class
    singleOf(::ScoreboardManager) bind IScoreboardManager::class
    singleOf(::StructureManager) bind IStructureManager::class
    singleOf(::TeamManager) bind ITeamManager::class
}
