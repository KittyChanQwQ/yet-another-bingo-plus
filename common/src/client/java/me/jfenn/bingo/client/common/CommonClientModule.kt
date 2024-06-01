package me.jfenn.bingo.client.common

import me.jfenn.bingo.api.ICloseEvent
import me.jfenn.bingo.client.common.hud.BingoHudController
import me.jfenn.bingo.client.common.hud.BingoHudRenderer
import me.jfenn.bingo.client.common.hud.BingoHudState
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.settings.ClientSettingsController
import me.jfenn.bingo.client.common.settings.ClientSettingsService
import me.jfenn.bingo.client.common.sound.ClientSounds
import me.jfenn.bingo.client.common.sound.ScorePacketReceiver
import me.jfenn.bingo.client.common.sound.SoundService
import me.jfenn.bingo.client.common.stats.ClientStatsSyncController
import me.jfenn.bingo.client.common.world.BingoWorldManager
import me.jfenn.bingo.client.impl.ClientCloseEvent
import me.jfenn.bingo.client.integrations.YetAnotherConfigLibIntegration
import me.jfenn.bingo.client.integrations.jei.JeiIntegration
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

val commonClientModule = module {
    singleOf(::ClientPacketEvents) withOptions { createdAtStart() }

    single { BingoHudState() }
    singleOf(::BingoHudRenderer)
    singleOf(::BingoHudController) withOptions { createdAtStart() }

    singleOf(::SoundService)
    singleOf(::ClientSounds) withOptions { createdAtStart() }
    singleOf(::ScorePacketReceiver) withOptions { createdAtStart() }

    singleOf(::ClientStatsSyncController) withOptions { createdAtStart() }

    singleOf(::BingoWorldManager) withOptions { createdAtStart() }

    singleOf(::ClientSettingsService)
    singleOf(::ClientSettingsController) withOptions { createdAtStart() }
    factoryOf(YetAnotherConfigLibIntegration::create)
    factoryOf(JeiIntegration::create)

    singleOf(::ResourcePackInit) withOptions { createdAtStart() }

    single { ClientCloseEvent } bind ICloseEvent::class
}
