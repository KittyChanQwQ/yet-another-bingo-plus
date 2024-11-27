package me.jfenn.bingo.client.entrypoint

import me.jfenn.bingo.client.integrations.YetAnotherConfigLibIntegration
import me.jfenn.bingo.common.scope.BingoKoin

internal object ModMenuEntrypointHelper {
    fun getYaclIntegration() = BingoKoin.koinApp.koin.get<YetAnotherConfigLibIntegration>()
}