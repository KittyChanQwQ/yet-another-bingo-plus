package me.jfenn.bingo.client.common

import me.jfenn.bingo.client.common.hud.BingoHudController
import me.jfenn.bingo.client.common.settings.ClientSettingsController
import me.jfenn.bingo.client.common.settings.ClientSettingsService
import me.jfenn.bingo.client.integrations.YetAnotherConfigLibIntegration
import me.jfenn.bingo.client.integrations.jei.JeiIntegration
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val commonClientModule = module {
    singleOf(::BingoHudController) withOptions { createdAtStart() }
    singleOf(::BingoWorldManager) withOptions { createdAtStart() }
    singleOf(::ClientSettingsService)
    singleOf(::ClientSettingsController) withOptions { createdAtStart() }
    factoryOf(YetAnotherConfigLibIntegration::create)
    factoryOf(JeiIntegration::create)
}
