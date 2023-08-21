package me.jfenn.bingo.card

import me.jfenn.bingo.config.Config
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.utils.lazyRef
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import javax.imageio.ImageIO

data class BingoCardEntry(
    val item: String,
) {

    val identifier get() = Identifier.of("minecraft", item.lowercase())!!

    val teams = HashSet<BingoTeam>()

    /**
     * Returns true if the item can be counted to complete the bingo tile
     *
     * Returns false if any of:
     * - item does not match the card item
     * - item has a bingo card's map ID
     * - item has a "bingo_ignore:true" NBT tag
     */
    fun canSatisfy(stack: ItemStack): Boolean {
        return stack.item == item() && !BingoMapService.isMapItem(stack) && stack.nbt?.getBoolean("bingo_ignore") != true
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
        return Registries.ITEM.get(identifier)
    }

    val imagePath: String = "images/${item.lowercase()}.png"

    val image by lazyRef {
        try {
            val inputStream = Config.readStream(imagePath, false) {
                javaClass.getResourceAsStream("/bingo/images/${item.lowercase()}.png")
                    ?: javaClass.getResourceAsStream("/assets/minecraft/textures/item/${item.lowercase()}.png")
                    ?: javaClass.getResourceAsStream("/assets/minecraft/textures/block/${item.lowercase()}.png")
                    ?: throw IllegalArgumentException("Cannot find default texture for ${item.lowercase()}.png")
            }

            ImageIO.read(inputStream)
        } catch (e: Throwable) {
            log.error("Error reading item image: $imagePath", e)
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

}
