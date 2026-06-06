package me.jfenn.bingo.client.integrations

import me.jfenn.bingo.platform.IModEnvironment

internal interface JourneymapIntegration {
    fun resetJourneymapData()

    companion object {
        fun hasJourneymap(environment: IModEnvironment) = environment.isModLoaded("journeymap")

        fun create(
            environment: IModEnvironment
        ): JourneymapIntegration {
            return if (hasJourneymap(environment)) {
                JourneymapIntegrationImpl()
            } else JourneymapIntegrationDummy()
        }
    }
}

internal class JourneymapIntegrationDummy : JourneymapIntegration {
    override fun resetJourneymapData() {}
}

internal class JourneymapIntegrationImpl : JourneymapIntegration {
    override fun resetJourneymapData() {

    }
}