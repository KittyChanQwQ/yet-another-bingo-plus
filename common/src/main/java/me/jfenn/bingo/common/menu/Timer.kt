package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

const val MAX_TIMER_MINUTES: Long = 360

fun MenuComponent.registerTimer(
    position: Vec3d,
    timerMinutesProp: MutableProperty<Duration?>,
    requiredPermissionLevel: Int = 2,
    title: String = "Time Limit",
) {
    var timerMinutes by timerMinutesProp

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        val mins = timerMinutes?.inWholeMinutes ?: 0
        val newMins = if (mins < MAX_TIMER_MINUTES)
            ((mins / 30) + 1) * 30
        else null
        timerMinutes = newMins?.minutes
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        val mins = timerMinutes?.inWholeMinutes ?: (MAX_TIMER_MINUTES + 30)
        val newMins = if (mins > 30)
            ((mins / 30) - 1) * 30
        else null
        timerMinutes = newMins?.minutes
    }

    registerEntity(EntityType.TEXT_DISPLAY, position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)) {
        val timerText = timerMinutes?.let {
            val minutes = it.inWholeMinutes
            String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
        } ?: "Off"

        putString(
            "text", TextInfo.toJsonString(
                TextInfo.literal(timerText)
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString(
            "text", TextInfo.toJsonString(
                TextInfo.literal(title)
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
