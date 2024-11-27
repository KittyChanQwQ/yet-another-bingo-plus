package me.jfenn.bingo.common.card.objective

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.platform.ITextSerializer
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.common.utils.jsonUnpretty
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class ObjectiveDisplayService(
    private val textSerializer: ITextSerializer,
    private val itemStackFactory: IItemStackFactory,
    private val log: Logger,
) {
    private fun insertSubstitutions(
        str: String,
        substitutions: Map<String, String>,
    ): String {
        var ret = str
        for ((key, value) in substitutions) {
            ret = ret.replace(key, value)
        }
        return ret
    }

    fun resolve(
        id: String,
        data: ObjectiveDisplay.Data?,
        fallback: ObjectiveDisplay.Resolved = ObjectiveDisplay.Resolved.EMPTY,
        substitutions: Map<String, String> = emptyMap(),
    ): ObjectiveDisplay.Resolved = run {
        try {
            val nameJson = data?.name?.let { jsonUnpretty.encodeToString(it) }
                ?: fallback.name?.let { textSerializer.toJson(it) }

            val name = nameJson?.let { insertSubstitutions(it, substitutions) }
                ?.let { textSerializer.fromJson(it) }

            val lore = data?.lore?.map {
                textSerializer.fromJson(jsonUnpretty.encodeToString(it))
            } ?: fallback.lore
            val item = data?.item
                ?.let {
                    try {
                        itemStackFactory.createStack(data.item)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                ?.also { stack ->
                    stack.setNbtString(data.itemNbt)
                    data.itemComponents?.let {
                        stack.setComponentsString(it.mapValues { (_, data) ->
                            jsonUnpretty.encodeToString(data)
                        })
                    }
                }
                ?: fallback.item

            ObjectiveDisplay.Resolved(
                name = name,
                lore = lore,
                item = item,
                decoration = data?.decoration ?: fallback.decoration,
                image = data?.image ?: fallback.image,
            )
        } catch (e: Throwable) {
            log.error("Unable to resolve objective display for '$id'", e)
            fallback
        }
    }.let { display ->
        display.copy(
            name = display.name?.let { name ->
                when {
                    name.string == display.item?.displayName?.string -> name
                    else -> Text.empty().append(name).formatted(Formatting.ITALIC)
                }
            },
            lore = display.lore?.map { Text.empty().append(it).formatted(Formatting.DARK_PURPLE) }
        )
    }
}