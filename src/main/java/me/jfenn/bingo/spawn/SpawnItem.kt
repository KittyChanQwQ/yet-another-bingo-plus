package me.jfenn.bingo.spawn

import kotlinx.serialization.Serializable
import me.jfenn.bingo.NBT_BINGO_IGNORE
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.nbt.visitor.StringNbtWriter
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

@Serializable
data class SpawnItem(
    val id: String,
    val nbt: String = "{}",
    val count: Int = 1,
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun fromItemStack(itemStack: ItemStack): SpawnItem {
            val identifier = Registries.ITEM.getId(itemStack.item)

            return SpawnItem(
                id = identifier.toString(),
                nbt = itemStack.getOrCreateNbt().toString(),
                count = itemStack.count,
            )
        }
    }

    fun toItemStacks(): List<ItemStack> {
        val identifier = Identifier.tryParse(id) ?: run {
            log.error("Cannot parse spawnEquipmentItems id '$id'")
            null
        } ?: return emptyList()

        val item = try {
            Registries.ITEM.get(identifier)
        } catch (e: Error) {
            log.error("spawnEquipmentItems id '$id' is not in the item registry", e)
            null
        } ?: return emptyList()

        val fullStackCount = count / item.maxCount
        val extraStackCount = count % item.maxCount

        val itemStacks = sequence {
            repeat(fullStackCount) {
                yield(ItemStack(item, item.maxCount))
            }

            if (extraStackCount > 0) {
                yield(ItemStack(item, extraStackCount))
            }
        }

        return itemStacks
            .map {
                try {
                    it.nbt = StringNbtReader.parse(nbt)
                } catch (e: Error) {
                    log.error("spawnEquipmentItems nbt '${nbt}' could not be parsed", e)
                }

                // set BINGO_IGNORE flag so that spawn items do not count on the bingo card
                it.getOrCreateNbt().apply {
                    putBoolean(NBT_BINGO_IGNORE, true)
                }

                it
            }
            .toList()
    }

}
