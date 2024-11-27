package me.jfenn.bingo.common.menu.tooltips

import me.jfenn.bingo.platform.IEntity
import net.minecraft.text.Text
import java.util.*

class TooltipService {

    private val registeredTooltips = mutableMapOf<UUID, List<Text>>()

    operator fun set(entity: IEntity, tooltip: List<Text>) {
        registeredTooltips[entity.uuid] = tooltip
    }

    operator fun get(uuid: UUID) = registeredTooltips[uuid]

}