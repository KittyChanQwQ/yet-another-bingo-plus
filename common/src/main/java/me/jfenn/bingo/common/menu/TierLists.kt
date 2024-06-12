package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.TierListService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f

fun MenuComponent.registerTierLists(
    position: Vec3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(text.string(StringKey.OptionsTierList)))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
        put("transformation", Matrix4f().scale(MENU_TEXT_SCALE).toNbt())
    }

    val pages = TierListService.readTierLists().chunked(4)

    // Update options.tierList/tierListPage to ensure that there is a tier list selected
    options.tierListPage = pages.indexOfFirst { it.contains(options.tierList) }
        .takeIf { it != -1 }
        ?: 0

    val page by computedProperty { options.tierListPage.coerceIn(pages.indices) }

    for (i in 0..3) {
        registerButton(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + i*MENU_ITEM_HEIGHT), 0.0),
            text = Text.literal(""),
            textProp = computedProperty {
                (pages[page].getOrNull(i)?.formatTitle() ?: "")
                    .let { Text.literal(it) }
            },
            isActiveProp = computedProperty { options.tierList == pages[page].getOrNull(i) },
            permissionGetter = permissionGetter,
        ) {
            val listName = pages[page].getOrNull(i) ?: return@registerButton
            options.tierList = listName
            markDirty()

            // when changed, update the bingo card
            cardService.generateCard()
        }
    }

    if (pages.size > 1) {
        registerButton(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + 4*MENU_ITEM_HEIGHT), 0.0),
            text = text.string(StringKey.OptionsTierListMore),
            isActiveProp = computedProperty { false },
            permissionGetter = permissionGetter,
        ) {
            options.tierListPage = (options.tierListPage + 1) % pages.size
            markDirty()
        }
    }
}