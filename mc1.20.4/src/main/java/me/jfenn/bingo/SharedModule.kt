package me.jfenn.bingo

import me.jfenn.bingo.api.*
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.api.particle.IParticleFactory
import me.jfenn.bingo.common.scope.BingoScope
import me.jfenn.bingo.impl.*
import me.jfenn.bingo.impl.particle.ParticleFactory
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val sharedModule = module {
    single { ParticleFactory } bind IParticleFactory::class
    singleOf(::AdvancementManager) bind IAdvancementManager::class
    singleOf(::CommandManagerImpl) bind ICommandManager::class
    singleOf(::CommandRunner) bind ICommandRunner::class
    single { EntityManagerImpl } bind IEntityManager::class
    single { Executors } bind IExecutors::class
    singleOf(::ItemStackFactory) bind IItemStackFactory::class
    singleOf(::MapColorServiceImpl) bind IMapColorService::class
    singleOf(::PersistentStateManager) bind IPersistentStateManager::class
    singleOf(::ScoreboardManager) bind IScoreboardManager::class
    single { ServerEventsImpl } bind IServerEvents::class
    single { ServerWorldImpl } bind IServerWorldFactory::class
    singleOf(::ServerNetworkingImpl) bind IServerNetworking::class
    singleOf(::StatManagerImpl) bind IStatManager::class
    singleOf(::TagAccessorImpl) bind ITagAccessor::class
    singleOf(::TeamManager) bind ITeamManager::class
    singleOf(::TextSerializer) bind ITextSerializer::class

    scope<BingoScope> {
        scopedOf(::PlayerManager) bind IPlayerManager::class
        scopedOf(::MinecraftServerImpl) binds arrayOf(IMapService::class, ITickManager::class, ILevelStorage::class)
        scopedOf(::RecipeManagerImpl) bind IRecipeManager::class
        scoped { Executors.createServerTaskExecutor(get()) }
    }
}
