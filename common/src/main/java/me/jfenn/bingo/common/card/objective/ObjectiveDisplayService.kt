package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.api.item.IItemStackFactory
import org.slf4j.Logger

class ObjectiveDisplayService(
    private val textSerializer: ITextSerializer,
    private val itemStackFactory: IItemStackFactory,
    private val log: Logger,
) {
    fun resolve(id: String, data: ObjectiveDisplay.Data): ObjectiveDisplay.Resolved? {
        return try {
            val name = data.name?.let { textSerializer.fromJson(it) }
            val lore = data.lore.map { textSerializer.fromJson(it) }
            val item = data.item?.let { itemStackFactory.createStack(data.item) }
            item?.setNbtString(data.itemNbt)
            data.itemComponents?.let { item?.setComponentsString(it) }

            ObjectiveDisplay.Resolved(
                name = name,
                lore = lore,
                item = item,
            )
        } catch (e: Throwable) {
            log.error("Unable to resolve objective display for '$id'", e)
            null
        }
    }
}