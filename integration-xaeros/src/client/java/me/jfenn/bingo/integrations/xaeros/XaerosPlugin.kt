package me.jfenn.bingo.integrations.xaeros

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import xaero.common.core.XaeroMinimapCore
import xaero.hud.minimap.world.MinimapWorld
import xaero.map.core.XaeroWorldMapCore
import xaero.map.file.MapSaveLoad
import java.io.IOException
import java.nio.file.Path
import java.util.function.Consumer


class XaerosPlugin : IXaerosPlugin {
    private val log = LoggerFactory.getLogger("bingo")

    override fun resetXaerosMapData() {
        val mapProcessor = XaeroWorldMapCore.currentSession.mapProcessor

        // Delete all map files
        deleteFolderFromDisk(MapSaveLoad.getRootFolder(mapProcessor.currentWorldId));
        // Force a reload from disk
        mapProcessor.mapWorld.currentDimension.clear();
    }

    override fun resetXaerosWaypointData() {
        val waypointSession = XaeroMinimapCore.currentSession.minimapProcessor.session.waypointSession
        val container = waypointSession.session.getWorldManager().currentRootContainer

        container.allWorldsIterable.forEach(Consumer { minimapWorld: MinimapWorld? ->
            minimapWorld!!.currentWaypointSet.clear()
            try {
                container.getSession().worldManagerIO.saveWorld(minimapWorld)
            } catch (_: IOException) {
                log.error("Failed to save map after deleting Xaeros waypoints! Waypoints will most likely stay behind!");
            }
        })
    }

    private fun deleteFolderFromDisk(path: Path) {
        if (FileUtils.isDirectory(path.toFile())) {
            try {
                log.info(String.format("Deleting Xaero's worldmap Folder at %s", path));
                FileUtils.deleteDirectory(path.toFile())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}