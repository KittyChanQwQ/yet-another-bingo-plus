package me.jfenn.bingo.client.common

import me.jfenn.bingo.client.common.hud.BingoHudController
import me.jfenn.bingo.client.common.hud.BingoHudRenderer
import me.jfenn.bingo.client.common.hud.BingoHudState
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.packet.ClientPacketManager
import me.jfenn.bingo.client.common.score.ScorePacketReceiver
import me.jfenn.bingo.client.common.score.ClientSounds
import me.jfenn.bingo.client.common.settings.ClientSettingsController
import me.jfenn.bingo.client.common.settings.ClientSettingsService
import me.jfenn.bingo.client.common.world.BingoWorldManager
import me.jfenn.bingo.client.common.world.BingoWorldService
import me.jfenn.bingo.client.common.world.BingoWorldState
import me.jfenn.bingo.client.integrations.YetAnotherConfigLibIntegration
import me.jfenn.bingo.client.integrations.jei.JeiIntegration
import net.minecraft.client.MinecraftClient
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val commonClientModule = module {
    singleOf(::ClientPacketManager)
    singleOf(::ClientPacketEvents) withOptions { createdAtStart() }

    single { BingoHudState() }
    singleOf(::BingoHudRenderer)
    singleOf(::BingoHudController) withOptions { createdAtStart() }

    singleOf(::ClientSounds) withOptions { createdAtStart() }
    singleOf(::ScorePacketReceiver) withOptions { createdAtStart() }

    single { BingoWorldState() }
    singleOf(::BingoWorldService)
    singleOf(::BingoWorldManager) withOptions { createdAtStart() }

    singleOf(::ClientSettingsService)
    singleOf(::ClientSettingsController) withOptions { createdAtStart() }
    factoryOf(YetAnotherConfigLibIntegration::create)
    factoryOf(JeiIntegration::create)

    singleOf(::ResourcePackInit) withOptions { createdAtStart() }
}
