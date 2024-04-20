package me.jfenn.bingo.common.spawn

import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.IItemStack
import me.jfenn.bingo.api.IItemStackFactory
import org.slf4j.LoggerFactory

@Serializable
data class SpawnItem(
    val id: String,
    val nbt: String? = null,
    val count: Int = 1,
) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun fromItemStack(itemStack: IItemStack): SpawnItem {
            return SpawnItem(
                id = itemStack.identifier.toString(),
                nbt = itemStack.getNbtString(),
                count = itemStack.count,
            )
        }
    }

    fun toItemStacks(
        itemStackFactory: IItemStackFactory,
    ): List<IItemStack> {
        val item = try {
            itemStackFactory.createStack(id)
        } catch (e: Throwable) {
            log.error("spawnEquipmentItems id '$id' is not in the item registry", e)
            return emptyList()
        }

        val fullStackCount = count / item.maxCount
        val extraStackCount = count % item.maxCount

        val itemStacks = sequence {
            repeat(fullStackCount) {
                yield(item.copy().apply { count = maxCount })
            }

            if (extraStackCount > 0) {
                yield(item.copy().apply { count = extraStackCount })
            }
        }.toList()

        if (nbt != null && nbt != "{}") {
            for (itemStack in itemStacks) {
                try {
                    itemStack.setNbtString(nbt)
                } catch (e: Error) {
                    log.error("spawnEquipmentItems nbt '${nbt}' could not be parsed", e)
                }
            }
        }

        return itemStacks
    }

}
