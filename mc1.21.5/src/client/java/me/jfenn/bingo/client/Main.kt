package me.jfenn.bingo.client

import me.jfenn.bingo.client.common.commonClientModule
import me.jfenn.bingo.client.impl.*
import me.jfenn.bingo.client.impl.event.ClientCloseEvent
import me.jfenn.bingo.client.impl.event.ClientEventsImpl
import me.jfenn.bingo.client.impl.DrawService
import me.jfenn.bingo.client.impl.screen.ScreenFactory
import me.jfenn.bingo.client.platform.*
import me.jfenn.bingo.client.platform.event.IClientEvents
import me.jfenn.bingo.client.platform.renderer.IDrawServiceFactory
import me.jfenn.bingo.client.platform.screen.IScreenFactory
import me.jfenn.bingo.client.world.BingoWorldController
import me.jfenn.bingo.client.world.BingoWorldState
import me.jfenn.bingo.client.world.WorldServiceImpl
import me.jfenn.bingo.common.commonInit
import me.jfenn.bingo.common.commonModule
import me.jfenn.bingo.common.config.MigrationHandler
import me.jfenn.bingo.platform.ICloseEvent
import me.jfenn.bingo.platform.scope.BingoKoin
import me.jfenn.bingo.sharedInit
import me.jfenn.bingo.sharedModule
import net.fabricmc.api.ClientModInitializer
import org.koin.core.logger.Level
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory

object Main : ClientModInitializer {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val clientModule = module {
        single { ClientCloseEvent } bind ICloseEvent::class
        single { ClientEventsImpl } bind IClientEvents::class
        singleOf(::ScreenFactory) bind IScreenFactory::class

        singleOf(::ButtonService) bind IButtonService::class
        singleOf(::ClientNetworkingImpl) bind IClientNetworking::class
        single { DrawService } bind IDrawServiceFactory::class
        singleOf(::HudCallbackImpl) bind IHudCallback::class
        singleOf(::KeyBindingManager) bind IKeyBindingManager::class
        single { NativeImageFactory } bind INativeImageFactory::class
        single { ScrollableWidgetFactory } bind IScrollableWidgetFactory::class
        single { OptionsAccessor } bind IOptionsAccessor::class
        singleOf(::ResourcePackManager) bind IResourcePackManager::class
        single { SessionAccessor } bind ISessionAccessor::class
        single { TabsWidgetFactory } bind ITabsWidgetFactory::class

        single { BingoWorldState() }
        singleOf(::WorldServiceImpl) bind IWorldService::class
        singleOf(::BingoWorldController) withOptions { createdAtStart() }
    }

    override fun onInitializeClient() {
        log.info("Starting Bingo migration runner...")

        koinApplication {
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(sharedModule, commonModule)
        }.also {
            it.koin.get<MigrationHandler>().runMigrations()
            it.close()
        }

        log.info("Starting Bingo Client application")
        sharedInit()

        koinApplication {
            BingoKoin.koinApp = this@koinApplication
            logger(SLF4JLogger(level = Level.DEBUG))
            modules(sharedModule, commonModule, clientModule, commonClientModule)
        }.also {
            it.koin.commonInit()
        }
    }
}