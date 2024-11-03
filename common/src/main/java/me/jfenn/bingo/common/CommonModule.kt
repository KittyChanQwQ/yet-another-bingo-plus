package me.jfenn.bingo.common

import me.jfenn.bingo.api.ICloseEvent
import me.jfenn.bingo.common.autorestart.ResetCommand
import me.jfenn.bingo.common.autorestart.ResetService
import me.jfenn.bingo.common.autorestart.RestartCommand
import me.jfenn.bingo.common.autorestart.RestartOnLeaveController
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.TagExpansionService
import me.jfenn.bingo.common.card.data.ObjectiveRegistryManager
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.card.filter.ObjectiveFilterCommand
import me.jfenn.bingo.common.card.filter.ObjectiveFilterService
import me.jfenn.bingo.common.card.objective.*
import me.jfenn.bingo.common.card.tag.*
import me.jfenn.bingo.common.card.tierlist.TierListManager
import me.jfenn.bingo.common.card.tierlist.TierListState
import me.jfenn.bingo.common.commands.*
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.MigrationHandler
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.controller.*
import me.jfenn.bingo.common.datapack.ServerDatapackManager
import me.jfenn.bingo.common.event.EventManager
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.game.BingoStartCommand
import me.jfenn.bingo.common.game.GameOverController
import me.jfenn.bingo.common.game.GameOverService
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.infobook.InfoBookCommand
import me.jfenn.bingo.common.infobook.InfoBookController
import me.jfenn.bingo.common.infobook.InfoBookService
import me.jfenn.bingo.common.map.*
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.menu.tooltips.TooltipController
import me.jfenn.bingo.common.menu.tooltips.TooltipService
import me.jfenn.bingo.common.ready.ReadyCommand
import me.jfenn.bingo.common.ready.ReadyController
import me.jfenn.bingo.common.ready.ReadyTimerState
import me.jfenn.bingo.common.scope.BingoScope
import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.scoreboard.ScoreboardController
import me.jfenn.bingo.common.scoreboard.ScoreboardService
import me.jfenn.bingo.common.scoring.ScoreUpdateService
import me.jfenn.bingo.common.scoring.ScoredItemCheck
import me.jfenn.bingo.common.spawn.*
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.*
import me.jfenn.bingo.common.team.OfflinePlayerCache
import me.jfenn.bingo.common.team.TeamController
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.timer.*
import me.jfenn.bingo.common.utils.Build
import me.jfenn.bingo.impl.ServerCloseEvent
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.integrations.VanishIntegration
import me.jfenn.bingo.integrations.chunky.ChunkyController
import me.jfenn.bingo.integrations.chunky.ChunkyIntegration
import me.jfenn.bingo.integrations.voice.VoiceGroupController
import me.jfenn.bingo.integrations.voice.VoiceIntegration
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
    singleOf(::MapColors)

    singleOf(::ConfigService)
    single { get<ConfigService>().config }
    single { get<ConfigService>().config.client }
    singleOf(::TextProvider)

    singleOf(::ServerDatapackManager)
    singleOf(::EventManager)
    singleOf(::ScopeManager)
    singleOf(::ServerPacketEvents)

    singleOf(::RestartOnLeaveController)
    factoryOf(PermissionsIntegration::create)
    factoryOf(VanishIntegration::create)
    factoryOf(ChunkyIntegration::create)
    factoryOf(VoiceIntegration::create)

    singleOf(::ReadyTimerState)

    singleOf(::BingoCommand)
    singleOf(::BingoCardCommand)
    singleOf(::BingoCommands)
    singleOf(::BingoDebugCommand)
    singleOf(::BingoLobbyCommand)
    singleOf(::BingoModeCommand)
    singleOf(::BingoOptionsCommands)
    singleOf(::BingoPrefsCommand)
    singleOf(::ResetCommand)
    singleOf(::BingoStartCommand)
    singleOf(::ChatCommand)
    singleOf(::CoordsCommand)
    singleOf(::JoinCommand)
    singleOf(::PreviewCardCommand)
    singleOf(::SpectatorCommand)
    singleOf(::ReadyCommand)
    singleOf(::RestartCommand)
    singleOf(::StatsCommand)
    singleOf(::ObjectiveFilterCommand)
    singleOf(::InfoBookCommand)

    singleOf(::ObjectiveRegistryState)
    singleOf(::ObjectiveRegistryManager)
    singleOf(::DataTagProvider)
    singleOf(::TagManager)
    singleOf(::TierListState)
    singleOf(::TierListManager)
    singleOf(::TagExpansionService)

    singleOf(::ConnectionFactory)
    single { get<ConnectionFactory>().create() }
    singleOf(::StatsService)

    scope<BingoScope> {
        scopedOf(::ScopedEvents)
        scopedOf(::InteractionEntityEvents)

        scoped { get<BingoScope>().server }
        scoped { get<BingoScope>().state }
        scoped { get<BingoState>().options }

        scopedOf(::ObjectiveService)
        scopedOf(::ObjectiveDisplayService)
        scopedOf(::AdvancementObjectiveManager)
        scopedOf(::ItemObjectiveManager)
        scopedOf(::AllOfObjectiveManager)
        scopedOf(::OneOfObjectiveManager)
        scopedOf(::InverseObjectiveManager)
        scopedOf(::OpponentObjectiveManager)
        scopedOf(::StatsObjectiveManager)
        scopedOf(::ScoreboardObjectiveManager)
        scoped {
            BingoObjectiveManager(
                listOf(
                    get<AdvancementObjectiveManager>(),
                    get<ItemObjectiveManager>(),
                    get<AllOfObjectiveManager>(),
                    get<OneOfObjectiveManager>(),
                    get<InverseObjectiveManager>(),
                    get<OpponentObjectiveManager>(),
                    get<StatsObjectiveManager>(),
                    get<ScoreboardObjectiveManager>(),
                ),
                get(),
                get(),
            )
        }

        scopedOf(::ObjectiveTagProvider)
        scopedOf(::TierListTagProvider)
        scoped {
            TagService(
                listOf(
                    get<DataTagProvider>(),
                    get<ObjectiveTagProvider>(),
                    get<TierListTagProvider>(),
                )
            )
        }
        scopedOf(::ObjectiveFilterService)
        scopedOf(::CardService)

        scopedOf(::StatsLobbyController)
        scopedOf(::StatsSyncController)
        scopedOf(::WriteStatsService)

        scopedOf(::GameService)
        scopedOf(::GameOverService)
        scopedOf(::GameOverController)

        scopedOf(::InfoBookController)
        scopedOf(::InfoBookService)

        scopedOf(::ResetService)
        scopedOf(::ReadyController)

        scopedOf(::MapItemService)
        scopedOf(::MapRenderService)
        scopedOf(::CardViewService)
        scopedOf(::CardImageService)
        scopedOf(::ScoreboardService)
        scopedOf(::PlayerSettingsService)

        scopedOf(::TooltipService)
        scopedOf(::TooltipController)
        scopedOf(::MenuController)

        scopedOf(::BingoMapController)
        scopedOf(::BossBarController)
        scopedOf(::DatapackFunctionService)
        scopedOf(::GameRuleController)
        scopedOf(::LobbyChaosController)
        scopedOf(::MapItemController)
        scopedOf(::MapItemHandler)
        scopedOf(::MotdController)
        scopedOf(::NightVisionController)
        scopedOf(::PlayerSettingsController)
        scopedOf(::PlayerSpectatorController)
        scopedOf(::ScoreboardController)

        scopedOf(::ChunkyController)
        scopedOf(::VoiceGroupController)

        scopedOf(::ChestService)
        scopedOf(::ChestController)
        scopedOf(::ElytraService)
        scopedOf(::SpawnService)
        scopedOf(SpreadPlayers::Factory)
        scopedOf(::PlayerController)
        scopedOf(::SpawnPreloadingController)

        scopedOf(::OfflinePlayerCache)
        scopedOf(::TeamService)
        scopedOf(::TeamController)

        scopedOf(::CountdownService)
        scopedOf(::CountdownController)
        scopedOf(::TimerCheck)
        scopedOf(::TimerPacketController)
        scopedOf(::WaitUntilLoadedController)

        scopedOf(::ScoredItemCheck)
        scopedOf(::ScoreUpdateService)

        scopedOf(::CommandTreeHandler)
    }
}

val commonServerModule = module {
    single { ServerCloseEvent } bind ICloseEvent::class
}

fun Koin.commonInit() {
    get<MapColors>()
    get<ServerDatapackManager>()
    get<EventManager>()
    get<ScopeManager>()
    get<ServerPacketEvents>()
    get<RestartOnLeaveController>()

    get<BingoCommand>()
    get<BingoCardCommand>()
    get<BingoCommands>()
    if (Build.isDebug)
        get<BingoDebugCommand>()
    get<BingoLobbyCommand>()
    get<BingoModeCommand>()
    get<BingoOptionsCommands>()
    get<BingoPrefsCommand>()
    get<ResetCommand>()
    get<BingoStartCommand>()
    get<ChatCommand>()
    get<CoordsCommand>()
    get<JoinCommand>()
    get<PreviewCardCommand>()
    get<SpectatorCommand>()
    get<ReadyCommand>()
    get<RestartCommand>()
    get<StatsCommand>()
    get<ObjectiveFilterCommand>()
    get<InfoBookCommand>()

    get<ObjectiveRegistryManager>()
    get<TagManager>()
    get<TierListManager>()

    get<ConnectionFactory>()
    get<StatsService>()
}

fun Scope.commonInit() {
    get<TooltipController>()
    get<MenuController>()

    get<BingoMapController>()
    get<BossBarController>()
    get<GameRuleController>()
    get<LobbyChaosController>()
    get<MapItemController>()
    get<MapItemHandler>()
    get<MotdController>()
    get<NightVisionController>()
    get<PlayerSettingsController>()
    get<PlayerSpectatorController>()
    get<ReadyController>()
    get<ScoreboardController>()

    get<GameOverController>()

    get<InfoBookController>()

    get<ScoredItemCheck>()

    get<StatsLobbyController>()
    get<StatsSyncController>()

    get<TeamController>()

    get<ChunkyController>()
    get<VoiceGroupController>()

    get<CountdownController>()
    get<TimerCheck>()
    get<TimerPacketController>()
    get<WaitUntilLoadedController>()

    get<CommandTreeHandler>()

    get<PlayerController>()
    get<ChestController>()
    get<SpawnPreloadingController>()
}
