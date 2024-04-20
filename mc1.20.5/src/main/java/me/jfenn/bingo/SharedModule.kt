package me.jfenn.bingo

import me.jfenn.bingo.api.*
import me.jfenn.bingo.impl.*
import me.jfenn.bingo.impl.networking.ServerNetworkingImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule = module {
    singleOf(::AdvancementManager) bind IAdvancementManager::class
    singleOf(::CommandRunner) bind ICommandRunner::class
    singleOf(::ItemStackFactory) bind IItemStackFactory::class
    singleOf(::MapServiceImpl) bind IMapService::class
    singleOf(::OfflinePlayerAccessor) bind IOfflinePlayerAccessor::class
    singleOf(::PersistentStateManager) bind IPersistentStateManager::class
    singleOf(::ScoreboardManager) bind IScoreboardManager::class
    singleOf(::ServerNetworkingImpl) bind IServerNetworking::class
    singleOf(::StructureManager) bind IStructureManager::class
    singleOf(::TeamManager) bind ITeamManager::class
    singleOf(::TextSerializer) bind ITextSerializer::class
    singleOf(::TickManagerImpl) bind ITickManager::class
}
