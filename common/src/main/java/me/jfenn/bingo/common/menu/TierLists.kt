package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerItemFilters(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    state: BingoState = koinScope.get(),
    config: BingoConfig = koinScope.get(),
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsTierList)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    val pages = config.itemFilters.entries.chunked(4)

    // Update options.tierList/tierListPage to ensure that there is a tier list selected
    options.tierListPage = pages
        .indexOfFirst { page ->
            page.any { it.value == options.cards.firstOrNull()?.itemFilter }
        }
        .takeIf { it != -1 }
        ?: 0

    val page by computedProperty { options.tierListPage.coerceIn(pages.indices) }

    for (i in 0..3) {
        val toggleProp = DelegatedProperty(
            getter = { state.getActiveCard().options.itemFilter == pages[page].getOrNull(i)?.value },
            setter = { isEnabled ->
                val filter = pages[page].getOrNull(i) ?: return@DelegatedProperty
                val cardOptions = state.getActiveCard().options
                cardOptions.itemFilter = filter.value
            }
        )

        registerToggleButton(
            position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + i*MENU_ITEM_HEIGHT), 0.0),
            text = Text.literal(""),
            textProp = computedProperty {
                // TODO: translations support
                pages[page].getOrNull(i)?.let { Text.literal(it.key.formatTitle()) }
            },
            toggleProp = toggleProp,
            permissionGetter = permissionGetter,
        ) {
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
            options.tierListPage = (options.tierListPage + 1) % pages.size
            markDirty()
        }
    }
}