package me.jfenn.bingo.client.entrypoint

import me.jfenn.bingo.client.integrations.YetAnotherConfigLibIntegration
import me.jfenn.bingo.common.scope.BingoKoin

object ModMenuEntrypointHelper {
    fun getYaclIntegration() = BingoKoin.koinApp.koin.get<YetAnotherConfigLibIntegration>()
}