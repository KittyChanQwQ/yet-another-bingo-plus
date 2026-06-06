package me.jfenn.bingo.client.integrations

import journeymap.client.task.main.DeleteMapTask
import journeymap.client.waypoint.WaypointHandler
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
        // Reset map data
        DeleteMapTask.queue(true);

        // Reset waypoints
        val dao = WaypointHandler.getInstance().dao;

        dao.waypoints.forEach { (_, wpImpl) ->
            run {
                dao.deleteWaypoint(wpImpl)
            }
        }

        dao.save(true)
    }
}