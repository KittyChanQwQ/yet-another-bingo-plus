package me.jfenn.bingo.spawn

import kotlinx.serialization.Serializable
import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.NBT_BINGO_IGNORE
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

@Serializable
data class SpawnItem(
    val id: String,
    val nbt: String? = null,
    val count: Int = 1,
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun fromItemStack(itemStack: ItemStack): SpawnItem {
            val identifier = Registries.ITEM.getId(itemStack.item)

            return SpawnItem(
                id = identifier.toString(),
                nbt = itemStack.nbt?.toString(),
                count = itemStack.count,
            )
        }
    }

    fun toIdentifier(): Identifier? {
        return Identifier.tryParse(id) ?: run {
            log.error("Cannot parse spawnEquipmentItems id '$id'")
            null
        }
    }

    fun toItemStacks(): List<ItemStack> {
        val identifier = toIdentifier() ?: return emptyList()

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
        }.toList()

        if (nbt != null && nbt != "{}") {
            for (itemStack in itemStacks) {
                try {
                    itemStack.nbt = StringNbtReader.parse(nbt)
                } catch (e: Error) {
                    log.error("spawnEquipmentItems nbt '${nbt}' could not be parsed", e)
                }
            }
        }

        return itemStacks
    }

}
