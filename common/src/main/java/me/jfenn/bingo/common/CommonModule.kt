package me.jfenn.bingo.common

import me.jfenn.bingo.api.BingoApiImpl
import me.jfenn.bingo.api.IBingoApi
import me.jfenn.bingo.common.autorestart.ResetCommand
import me.jfenn.bingo.common.autorestart.ResetService
import me.jfenn.bingo.common.autorestart.RestartOnLeaveController
import me.jfenn.bingo.common.bossbar.BossBarController
import me.jfenn.bingo.common.bossbar.BossBarService
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.TagExpansionService
import me.jfenn.bingo.common.card.filter.ObjectiveFilterCommand
import me.jfenn.bingo.common.card.filter.ObjectiveFilterService
import me.jfenn.bingo.common.card.objective.*
import me.jfenn.bingo.common.card.tag.DataTagProvider
import me.jfenn.bingo.common.card.tag.ObjectiveTagProvider
import me.jfenn.bingo.common.card.tag.TagService
import me.jfenn.bingo.common.card.tag.TierListTagProvider
import me.jfenn.bingo.common.chat.ChatCommand
import me.jfenn.bingo.common.chat.ChatMessageService
import me.jfenn.bingo.common.chat.CoordsCommand
import me.jfenn.bingo.common.commands.*
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.MigrationHandler
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.controller.*
import me.jfenn.bingo.common.data.*
import me.jfenn.bingo.common.datapack.ServerDatapackManager
import me.jfenn.bingo.common.event.EventManager
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.game.GameCommands
import me.jfenn.bingo.common.game.GameOverController
import me.jfenn.bingo.common.game.GameOverService
import me.jfenn.bingo.common.game.GameService
import me.jfenn.bingo.common.infobook.InfoBookCommand
import me.jfenn.bingo.common.infobook.InfoBookController
import me.jfenn.bingo.common.infobook.InfoBookService
import me.jfenn.bingo.common.lobby.BingoLobbyCommand
import me.jfenn.bingo.common.lobby.LobbyModeController
import me.jfenn.bingo.common.lobby.LobbyModeService
import me.jfenn.bingo.common.map.*
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.menu.tooltips.TooltipController
import me.jfenn.bingo.common.menu.tooltips.TooltipService
import me.jfenn.bingo.common.ready.ReadyCommand
import me.jfenn.bingo.common.ready.ReadyController
import me.jfenn.bingo.common.ready.ReadyService
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
import me.jfenn.bingo.common.team.*
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
import me.jfenn.bingo.platform.ICloseEvent
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

    factoryOf(PermissionsIntegration::create)
    factoryOf(VanishIntegration::create)
    factoryOf(ChunkyIntegration::create)
    factoryOf(VoiceIntegration::create)

    singleOf(::ReadyTimerState)

    singleOf(::BingoCommand)
    singleOf(::BingoCardCommand)
    singleOf(::BingoDebugCommand)
    singleOf(::BingoLobbyCommand)
    singleOf(::BingoModeCommand)
    singleOf(::BingoOptionsCommands)
    singleOf(::BingoPrefsCommand)
    singleOf(::ResetCommand)
    singleOf(::GameCommands)
    singleOf(::ChatCommand)
    singleOf(::CoordsCommand)
    singleOf(::JoinCommand)
    singleOf(::ShuffleTeamsCommand)
    singleOf(::SpectatorCommand)
    singleOf(::ReadyCommand)
    singleOf(::StatsCommand)
    singleOf(::ObjectiveFilterCommand)
    singleOf(::InfoBookCommand)

    singleOf(::TagExpansionService)

    singleOf(::ConnectionFactory)
    single { get<ConnectionFactory>().create() }
    singleOf(::StatsService)

    scope<BingoScope> {
        scopedOf(::ScopedEvents)
        scopedOf(::InteractionEntityEvents)
        scopedOf(::BingoApiImpl) bind IBingoApi::class

        scoped { get<BingoScope>().server }
        scoped { get<BingoScope>().state }
        scoped { get<BingoState>().options }

        scopedOf(::ObjectiveLoader)
        scopedOf(::TierListLoader)
        scopedOf(::TagLoader)
        scopedOf(::UnobtainableItemsLoader)
        scopedOf(::ScopedData)
        scopedOf(::DataLoaderManager)

        scopedOf(::ObjectiveService)
        scopedOf(::ObjectiveDisplayService)
        scopedOf(::AdvancementObjectiveManager)
        scopedOf(::ItemObjectiveManager)
        scopedOf(::SomeOfObjectiveManager)
        scopedOf(::InverseObjectiveManager)
        scopedOf(::OpponentObjectiveManager)
        scopedOf(::StatsObjectiveManager)
        scopedOf(::ScoreboardObjectiveManager)
        scoped {
            BingoObjectiveManager(
                listOf(
                    get<AdvancementObjectiveManager>(),
                    get<ItemObjectiveManager>(),
                    get<SomeOfObjectiveManager>(),
                    get<InverseObjectiveManager>(),
                    get<OpponentObjectiveManager>(),
                    get<StatsObjectiveManager>(),
                    get<ScoreboardObjectiveManager>(),
                ),
                get(),
                get(),
            )
        }

        scopedOf(::DataTagProvider)
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

        scopedOf(::OptionsService)
        scopedOf(::ObjectiveFilterService)
        scopedOf(::CardService)

        scopedOf(::LobbyModeService)
        scopedOf(::LobbyModeController)

        scopedOf(::StatsLobbyController)
        scopedOf(::StatsSyncController)
        scopedOf(::WriteStatsService)

        scopedOf(::GameService)
        scopedOf(::GameOverService)
        scopedOf(::GameOverController)

        scopedOf(::InfoBookController)
        scopedOf(::InfoBookService)

        scopedOf(::ResetService)
        scopedOf(::ReadyService)
        scopedOf(::ReadyController)

        scopedOf(::ChatMessageService)

        scopedOf(::MapItemService)
        scopedOf(::MapRenderService)
        scopedOf(::CardViewService)
        scopedOf(::CardImageService)
        scopedOf(::ScoreboardService)
        scopedOf(::PlayerSettingsService)

        scopedOf(::ScoredItemCheck)
        scopedOf(::ScoreUpdateService)

        scopedOf(::TooltipService)
        scopedOf(::TooltipController)
        scopedOf(::MenuController)

        scopedOf(::BossBarService)
        scopedOf(::BossBarController)

        scopedOf(::BingoMapController)
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

        scopedOf(::RestartOnLeaveController)

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

    get<BingoCommand>()
    get<BingoCardCommand>()
    if (Build.isDebug)
        get<BingoDebugCommand>()
    get<BingoLobbyCommand>()
    get<BingoModeCommand>()
    get<BingoOptionsCommands>()
    get<BingoPrefsCommand>()
    get<ResetCommand>()
    get<GameCommands>()
    get<ChatCommand>()
    get<CoordsCommand>()
    get<JoinCommand>()
    get<ShuffleTeamsCommand>()
    get<SpectatorCommand>()
    get<ReadyCommand>()
    get<StatsCommand>()
    get<ObjectiveFilterCommand>()
    get<InfoBookCommand>()

    get<ConnectionFactory>()
    get<StatsService>()
}

fun Scope.commonInit() {
    get<DataLoaderManager>()

    get<ScoredItemCheck>() // must be registered before MenuController

    get<TooltipController>()
    get<MenuController>()
    get<InfoBookController>()

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

    get<LobbyModeController>()

    get<StatsLobbyController>()
    get<StatsSyncController>()

    get<TeamController>()

    get<ChunkyController>()
    get<VoiceGroupController>()

    get<CountdownController>()
    get<TimerCheck>()
    get<TimerPacketController>()
    get<WaitUntilLoadedController>()

    get<RestartOnLeaveController>()

    get<CommandTreeHandler>()

    get<PlayerController>()
    get<ChestController>()
    get<SpawnPreloadingController>()
}
