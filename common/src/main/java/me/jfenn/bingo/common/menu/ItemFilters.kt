package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.filter.ObjectiveFilterService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d

private var itemFilterPage: Int = 0

fun MenuComponent.registerItemFilters(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    state: BingoState = koinScope.get(),
    cardService: CardService = koinScope.get(),
    objectiveFilterService: ObjectiveFilterService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsFilter)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    val pages = objectiveFilterService.getPresetFilters().entries.chunked(4)

    // Update options.tierList/tierListPage to ensure that any selected item is visible
    itemFilterPage = pages
        .indexOfFirst { page ->
            page.any { it.value == options.cards.firstOrNull()?.itemFilter }
        }
        .takeIf { it != -1 }
        ?: 0

    val page by computedProperty { itemFilterPage.coerceIn(pages.indices) }

    for (i in 0..3) {
        registerButton(
            position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + i*MENU_ITEM_HEIGHT), 0.0),
            text = Text.literal(""),
            textProp = computedProperty {
                pages[page].getOrNull(i)?.let { ObjectiveFilterList.formatName(it.key) }
                    ?: Text.empty()
            },
            isActiveProp = computedProperty {
                state.getActiveCard().options.itemFilter == pages[page].getOrNull(i)?.value
            },
            permissionGetter = permissionGetter,
        ) {
            val filter = pages[page].getOrNull(i) ?: return@registerButton
            val cardOptions = state.getActiveCard().options
            cardOptions.itemFilter = filter.value
            markDirty()

            // when changed, update the bingo card
            cardService.generateCard()
        }
    }

    if (pages.size > 1) {
        registerButton(
            position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 4*MENU_ITEM_HEIGHT), 0.0),
            text = text.string(StringKey.OptionsMore),
            textProp = computedProperty {
                Text.literal(
                    pages.indices
                        .joinToString(" ") { index ->
                            val hasSelected = pages[index].any { it.value == state.getActiveCard().options.itemFilter }
                            when {
                                page == index -> if (hasSelected) "■" else "□"
                                else -> if (hasSelected) "◆" else "◇"
                            }
                        }
                            + " »"
                )
            },
            isActiveProp = computedProperty { false },
            permissionGetter = permissionGetter,
        ) {
            itemFilterPage = (itemFilterPage + 1) % pages.size
            markDirty()
        }
    }
}