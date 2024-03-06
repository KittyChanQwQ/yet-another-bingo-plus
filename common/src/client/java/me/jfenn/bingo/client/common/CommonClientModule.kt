package me.jfenn.bingo.client.common

import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val commonClientModule = module {
    singleOf(::BingoHudController) withOptions { createdAtStart() }
    singleOf(::BingoWorldManager) withOptions { createdAtStart() }
}
