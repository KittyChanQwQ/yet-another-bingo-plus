package me.jfenn.bingo.common

import me.jfenn.bingo.api.ICloseEvent
import me.jfenn.bingo.common.autorestart.RestartCommand
import me.jfenn.bingo.common.autorestart.RestartOnLeaveController
import me.jfenn.bingo.common.autorestart.RestartTimerController
import me.jfenn.bingo.common.autorestart.RestartTimerState
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.commands.*
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.MigrationHandler
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.config.TierListService
import me.jfenn.bingo.common.controller.*
import me.jfenn.bingo.common.datapack.ServerDatapackManager
import me.jfenn.bingo.common.event.EventManager
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.game.GameOverController
import me.jfenn.bingo.common.game.GameOverService
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.map.BingoMapController
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.map.MapItemService
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.scope.BingoScope
import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.scoreboard.ScoreboardController
import me.jfenn.bingo.common.scoreboard.ScoreboardService
import me.jfenn.bingo.common.scoring.ScoreUpdateService
import me.jfenn.bingo.common.scoring.ScoredItemCheck
import me.jfenn.bingo.common.spawn.ChestController
import me.jfenn.bingo.common.spawn.ChestService
import me.jfenn.bingo.common.spawn.PlayerController
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.ConnectionFactory
import me.jfenn.bingo.common.stats.StatsService
import me.jfenn.bingo.common.stats.StatsSyncController
import me.jfenn.bingo.common.stats.WriteStatsController
import me.jfenn.bingo.common.team.TeamController
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.timer.*
import me.jfenn.bingo.impl.ServerCloseEvent
import me.jfenn.bingo.integrations.ChunkyIntegration
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.integrations.VanishIntegration
import org.koin.core.Koin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import org.slf4j.LoggerFactory

val commonModule = module {
    single { getKoin() }
    single { LoggerFactory.getLogger(MOD_ID) }
    singleOf(::MigrationHandler)

    singleOf(::ConfigService)
    single { get<ConfigService>().config }
    single { get<ConfigService>().config.client }
    singleOf(::TierListService)
    singleOf(::TextProvider)

    singleOf(::ServerDatapackManager)
    singleOf(::EventManager)
    singleOf(::ScopeManager)
    singleOf(::ServerPacketEvents)

    singleOf(::RestartOnLeaveController)
    factoryOf(PermissionsIntegration::create)
    factoryOf(VanishIntegration::create)
    factoryOf(ChunkyIntegration::create)

    singleOf(::BingoCommands)
    singleOf(::BingoPrefsCommand)
    singleOf(::ChatCommand)
    singleOf(::CoordsCommand)
    singleOf(::JoinCommand)
    singleOf(::SpectatorCommand)
    singleOf(::RestartCommand)
    singleOf(::BingoStatsCommand)

    singleOf(::ConnectionFactory)
    single { get<ConnectionFactory>().create() }
    singleOf(::StatsService)

    scope<BingoScope> {
        scopedOf(::ScopedEvents)
        scopedOf(::InteractionEntityEvents)

        scoped { get<BingoScope>().server }
        scoped { get<BingoScope>().state }
        scoped { get<BingoState>().options }

        scopedOf(::StatsSyncController)
        scopedOf(::WriteStatsController)

        scopedOf(::GameService)
        scopedOf(::GameOverService)
        scopedOf(::GameOverController)

        scopedOf(::MapItemService)
        scopedOf(::MapRenderService)
        scopedOf(::CardViewService)
        scopedOf(::CardService)
        scopedOf(::ScoreboardService)
        scopedOf(::PlayerSettingsService)

        scopedOf(::MenuController)
        scopedOf(::BingoMapController)
        scopedOf(::BossBarController)
        scopedOf(::GameRuleController)
        scopedOf(::IntegrationController)
        scopedOf(::LobbyChaosController)
        scopedOf(::MapItemController)
        scopedOf(::MapItemHandler)
        scopedOf(::MotdController)
        scopedOf(::NightVisionController)
        scopedOf(::PlayerSettingsController)
        scopedOf(::PlayerSpectatorController)
        scopedOf(::ScoreboardController)

        scopedOf(::ChestService)
        scopedOf(::ChestController)
        scopedOf(::SpawnService)
        scopedOf(::PlayerController)

        scopedOf(::TeamService)
        scopedOf(::TeamController)

        scopedOf(::CountdownService)
        scopedOf(::CountdownController)
        scopedOf(::TimerCheck)
        scopedOf(::TimerPacketController)
        scopedOf(::WaitUntilLoadedController)

        scopedOf(::ScoredItemCheck)
        scopedOf(::ScoreUpdateService)

        scoped { RestartTimerState() }
        scopedOf(::RestartTimerController)

        scopedOf(::CommandTreeHandler)
    }
}

val commonServerModule = module {
    single { ServerCloseEvent } bind ICloseEvent::class
}

fun Koin.commonInit() {
    get<ServerDatapackManager>()
    get<EventManager>()
    get<ScopeManager>()
    get<ServerPacketEvents>()
    get<RestartOnLeaveController>()

    get<BingoCommands>()
    get<BingoPrefsCommand>()
    get<ChatCommand>()
    get<CoordsCommand>()
    get<JoinCommand>()
    get<SpectatorCommand>()
    get<RestartCommand>()
    get<BingoStatsCommand>()

    get<ConnectionFactory>()
    get<StatsService>()
}

fun Scope.commonInit() {
    get<MenuController>()
    get<BingoMapController>()
    get<BossBarController>()
    get<GameRuleController>()
    get<IntegrationController>()
    get<LobbyChaosController>()
    get<MapItemController>()
    get<MapItemHandler>()
    get<MotdController>()
    get<NightVisionController>()
    get<PlayerSettingsController>()
    get<PlayerSpectatorController>()
    get<ScoreboardController>()

    get<GameOverController>()

    get<ScoredItemCheck>()

    get<StatsSyncController>()
    get<WriteStatsController>()

    get<TeamController>()

    get<CountdownController>()
    get<TimerCheck>()
    get<TimerPacketController>()
    get<WaitUntilLoadedController>()

    get<RestartTimerController>()
    get<CommandTreeHandler>()

    get<PlayerController>()
    get<ChestController>()
}
