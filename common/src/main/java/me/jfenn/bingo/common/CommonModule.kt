package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.commands.*
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.MigrationHandler
import me.jfenn.bingo.common.config.TierListService
import me.jfenn.bingo.common.controller.*
import me.jfenn.bingo.common.datapack.ServerDatapackManager
import me.jfenn.bingo.common.event.EventManager
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.map.MapItemService
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.scope.BingoContext
import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.scoreboard.ScoreboardService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.SerializedPersistentStateFactory
import me.jfenn.bingo.integrations.ChunkyIntegration
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.integrations.VanishIntegration
import net.minecraft.server.MinecraftServer
import org.koin.core.Koin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.scope.Scope
import org.koin.dsl.module
import org.slf4j.LoggerFactory

val commonModule = module {
    single { getKoin() }
    single { LoggerFactory.getLogger(MOD_ID) }
    single { SerializedPersistentStateFactory(get()) }
    singleOf(::MigrationHandler)

    singleOf(::ConfigService)
    single { get<ConfigService>().config }
    singleOf(::TierListService)

    singleOf(::ServerDatapackManager)
    singleOf(::EventManager)
    singleOf(::ScopeManager)

    singleOf(::AutoShutdownController)
    factoryOf(PermissionsIntegration::create)
    factoryOf(VanishIntegration::create)
    factoryOf(ChunkyIntegration::create)

    singleOf(::BingoCommands)
    singleOf(::ChatCommand)
    singleOf(::CoordsCommand)
    singleOf(::JoinCommand)
    singleOf(::SpectatorCommand)
    singleOf(::RestartCommand)

    scope<BingoContext> {
        scopedOf(::ScopedEvents)
        scopedOf(::InteractionEntityEvents)

        scoped { get<BingoContext>().server }
        scoped {
            val world = get<MinecraftServer>().overworld
            get<SerializedPersistentStateFactory>().fromWorld<BingoState>(world)
        }
        scoped { get<BingoState>().options }

        scopedOf(::GameService)
        scopedOf(::SpawnService)
        scopedOf(::MapItemService)
        scopedOf(::MapRenderService)
        scopedOf(::CardViewService)
        scopedOf(::TeamService)
        scopedOf(::CardService)
        scopedOf(::ScoreboardService)

        scopedOf(::MenuController)
        scopedOf(::BingoCardController)
        scopedOf(::BossBarController)
        scopedOf(::GameRuleController)
        scopedOf(::IntegrationController)
        scopedOf(::LobbyChaosController)
        scopedOf(::MapItemHandler)
        scopedOf(::MotdController)
        scopedOf(::PlayerController)
        scopedOf(::PlayerSettingsController)
        scopedOf(::PlayerSpectatorController)
        scopedOf(::ScoreboardController)
        scopedOf(::ScoredItemCheck)
        scopedOf(::TimerCheck)

        scopedOf(::CommandTreeHandler)
    }
}

fun Koin.commonInit() {
    get<ServerDatapackManager>()
    get<EventManager>()
    get<ScopeManager>()
    get<AutoShutdownController>()

    get<BingoCommands>()
    get<ChatCommand>()
    get<CoordsCommand>()
    get<JoinCommand>()
    get<SpectatorCommand>()
    get<RestartCommand>()
}

fun Scope.commonInit() {
    get<MenuController>()
    get<BingoCardController>()
    get<BossBarController>()
    get<GameRuleController>()
    get<IntegrationController>()
    get<LobbyChaosController>()
    get<MapItemHandler>()
    get<MotdController>()
    get<PlayerController>()
    get<PlayerSettingsController>()
    get<PlayerSpectatorController>()
    get<ScoreboardController>()
    get<ScoredItemCheck>()
    get<TimerCheck>()
    get<CommandTreeHandler>()
}
