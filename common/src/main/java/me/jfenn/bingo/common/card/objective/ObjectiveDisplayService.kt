package me.jfenn.bingo.common.card.objective

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.utils.jsonUnpretty
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class ObjectiveDisplayService(
    private val textSerializer: ITextSerializer,
    private val itemStackFactory: IItemStackFactory,
    private val log: Logger,
) {
    fun resolve(
        id: String,
        data: ObjectiveDisplay.Data?,
        fallback: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY
    ): ObjectiveDisplay.Resolved = run {
        if (data == null) return@run fallback

        try {
            val name = data.name?.let {
                textSerializer.fromJson(jsonUnpretty.encodeToString(it))
            } ?: fallback.name
            val lore = data.lore?.map {
                textSerializer.fromJson(jsonUnpretty.encodeToString(it))
            } ?: fallback.lore
            val item = data.item?.let {
                val stack = itemStackFactory.createStack(data.item)
                stack.setNbtString(data.itemNbt)
                data.itemComponents?.let {
                    stack.setComponentsString(it.mapValues { (_, data) ->
                        jsonUnpretty.encodeToString(data)
                    })
                }
                stack
            } ?: fallback.item

            ObjectiveDisplay.Resolved(
                name = name,
                lore = lore,
                item = item,
            )
        } catch (e: Throwable) {
            log.error("Unable to resolve objective display for '$id'", e)
            fallback
        }
    }.let { display ->
        display.copy(
            name = display.name?.let { name ->
                when {
                    name == display.item?.displayName -> name
                    else -> Text.empty().append(name).formatted(Formatting.ITALIC)
                }
            },
            lore = display.lore?.map { Text.empty().append(it).formatted(Formatting.DARK_PURPLE) }
        )
    }
}