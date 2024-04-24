package me.jfenn.bingo

import me.jfenn.bingo.api.*
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.scope.BingoContext
import me.jfenn.bingo.impl.*
import me.jfenn.bingo.impl.networking.ServerNetworkingImpl
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val sharedModule = module {
    singleOf(::ServerNetworkingImpl) bind IServerNetworking::class

    singleOf(::AdvancementManager) bind IAdvancementManager::class
    singleOf(::CommandRunner) bind ICommandRunner::class
    singleOf(::ItemStackFactory) bind IItemStackFactory::class
    singleOf(::PersistentStateManager) bind IPersistentStateManager::class
    singleOf(::ScoreboardManager) bind IScoreboardManager::class
    single { StatusEffectsImpl } bind IStatusEffectsFactory::class
    singleOf(::StructureManager) bind IStructureManager::class
    singleOf(::TeamManager) bind ITeamManager::class
    singleOf(::TextSerializer) bind ITextSerializer::class

    scope<BingoContext> {
        scopedOf(::PlayerManager) bind IPlayerManager::class
        scopedOf(::MinecraftServerImpl) binds arrayOf(IMapService::class, ITickManager::class)
    }
}
