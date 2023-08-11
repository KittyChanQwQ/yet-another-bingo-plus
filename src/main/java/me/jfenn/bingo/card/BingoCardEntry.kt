package me.jfenn.bingo.card

import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.utils.lazyRef
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.util.log.Log
import net.fabricmc.loader.impl.util.log.LogCategory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.nio.file.Path
import javax.imageio.ImageIO

class BingoCardEntry(
    val item: String,
) {

    val teams = HashSet<BingoTeam>()

    /**
     * Returns true if the item can be counted to complete the bingo tile
     */
    fun canSatisfy(stack: ItemStack): Boolean {
        return stack.item == item() && !BingoMapService.isMapItem(stack)
    }

    fun hasAchieved(team: BingoTeam): Boolean {
        return teams.contains(team)
    }

    fun setAchieved(team: BingoTeam, value: Boolean): Boolean {
        return if (value) {
            teams.add(team)
        } else {
            teams.remove(team)
        }
    }

    fun item(): Item {
        return Registries.ITEM[Identifier.of("minecraft", item.lowercase())]
    }

    private val imagePath: Path = FabricLoader.getInstance().configDir
        .resolve("bingo/item_data/images/${item.lowercase()}.png")

    val image by lazyRef {
        try {
            ImageIO.read(imagePath.toFile())
        } catch (e: Exception) {
            Log.error(LogCategory.LOG, "Error reading item image: $imagePath")
            e.printStackTrace()
            null
        }
    }

}
