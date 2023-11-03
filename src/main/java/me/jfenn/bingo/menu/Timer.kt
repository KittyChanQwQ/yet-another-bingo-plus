package me.jfenn.bingo.menu

import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import java.time.Duration
import java.util.*

const val MAX_TIMER_MINUTES = 360

fun MenuComponent.registerTimer(
    position: Vec3d,
    timerMinutesProp: MutableProperty<Int?>,
    requiredPermissionLevel: Int = 2,
    title: String = "Time Limit",
) {
    var timerMinutes by property(timerMinutesProp)

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        timerMinutes = (timerMinutes ?: 0).let { mins ->
            if (mins < MAX_TIMER_MINUTES)
                ((mins / 30) + 1) * 30
            else null
        }
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        requiredPermissionLevel = requiredPermissionLevel,
    ) {
        timerMinutes = (timerMinutes ?: (MAX_TIMER_MINUTES + 30)).let { mins ->
            if (mins > 30)
                ((mins / 30) - 1) * 30
            else null
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        val timerText = timerMinutes?.let {
            val duration = Duration.ofMinutes(it.toLong())
            String.format(Locale.US, "%dh %02dm", duration.toHoursPart(), duration.toMinutesPart())
        } ?: "Off"

        put("Pos", position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0).toNbt())
        putString(
            "text", Text.Serializer.toJson(
                Text.literal(timerText)
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.toNbt())
        putString(
            "text", Text.Serializer.toJson(
                Text.literal(title)
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
