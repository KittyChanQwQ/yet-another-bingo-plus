package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.datapack.ServerDatapackManager
import me.jfenn.bingo.common.event.EventManager
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.integrations.*
import me.jfenn.bingo.common.map.MapItemService
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.menu.MenuController
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.SerializedPersistentStateFactory
import net.minecraft.server.MinecraftServer
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.scope.Scope
import org.koin.dsl.module
import org.slf4j.LoggerFactory

val commonModule = module {
    single { getKoin() }
    single { LoggerFactory.getLogger(MOD_ID) }
    single { SerializedPersistentStateFactory(get()) }

    singleOf(::ServerDatapackManager) withOptions { createdAtStart() }
    singleOf(::EventManager) withOptions { createdAtStart() }
    singleOf(::ScopeManager) withOptions { createdAtStart() }

    scope<BingoContext> {
        scoped { ScopedEvents() }
        scoped { InteractionEntityEvents() }

        scoped { get<BingoContext>().server }
        scoped {
            val world = get<MinecraftServer>().overworld
            get<SerializedPersistentStateFactory>().fromWorld<BingoState>(world)
        }
        scoped { ConfigService.config.copy() }

        scopedOf(::GameService)
        scopedOf(::SpawnService)
        scopedOf(::MapItemService)
        scopedOf(::MapRenderService)
        scopedOf(::TeamService)
        scopedOf(::CardService)

        scopedOf(::MenuController)
        scopedOf(::BingoCardController)
        scopedOf(::BossBarController)
        scopedOf(::GameRuleController)
        scopedOf(::MapItemHandler)
        scopedOf(::MotdController)
        scopedOf(::PlayerController)
        scopedOf(::ScoreboardHandler)
        scopedOf(::ScoreboardController)
        scopedOf(::ScoredItemCheck)
        scopedOf(::TimerCheck)
    }
}

fun Scope.commonInit() {
    get<MenuController>()
    get<BingoCardController>()
    get<BossBarController>()
    get<GameRuleController>()
    get<MapItemHandler>()
    get<MotdController>()
    get<PlayerController>()
    get<ScoreboardController>()
    get<ScoredItemCheck>()
    get<TimerCheck>()
}
