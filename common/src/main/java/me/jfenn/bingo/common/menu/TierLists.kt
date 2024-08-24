package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.tierlist.TierListService
import me.jfenn.bingo.common.card.tierlist.TierListState
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerTierLists(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
    tierLists: TierListState = koinScope.get(),
    tierListService: TierListService = koinScope.get(),
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

    val pages = tierLists.tierLists.keys.chunked(4)

    // Update options.tierList/tierListPage to ensure that there is a tier list selected
    options.tierListPage = pages.indexOfFirst { it.contains(options.tierLists.keys.firstOrNull()) }
        .takeIf { it != -1 }
        ?: 0

    val page by computedProperty { options.tierListPage.coerceIn(pages.indices) }

    for (i in 0..3) {
        val toggleProp = DelegatedProperty(
            getter = { options.tierLists.contains(pages[page].getOrNull(i)) },
            setter = { isEnabled ->
                val list = pages[page].getOrNull(i) ?: return@DelegatedProperty
                when {
                    isEnabled -> options.tierLists += (list to 1)
                    options.tierLists.size > 1 -> options.tierLists -= list
                }
            }
        )

        registerToggleButton(
            position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + i*MENU_ITEM_HEIGHT), 0.0),
            text = Text.literal(""),
            textProp = computedProperty {
                pages[page].getOrNull(i)?.let { tierListService.getName(it) }
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
            text = text.string(StringKey.OptionsTierListMore),
            textProp = computedProperty {
                Text.literal(
                    pages.indices
                        .joinToString(" ") {
                            val hasSelected = pages[it].any { options.tierLists.containsKey(it) }
                            when {
                                page == it -> if (hasSelected) "■" else "□"
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