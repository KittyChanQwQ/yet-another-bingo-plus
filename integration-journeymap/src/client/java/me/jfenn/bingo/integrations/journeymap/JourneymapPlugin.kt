package me.jfenn.bingo.integrations.journeymap

import journeymap.client.task.main.DeleteMapTask
import journeymap.client.waypoint.WaypointHandler
import kotlin.collections.component1
import kotlin.collections.component2

class JourneymapPlugin : IJourneymapPlugin {
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