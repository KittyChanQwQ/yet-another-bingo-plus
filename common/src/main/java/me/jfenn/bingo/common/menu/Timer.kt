package me.jfenn.bingo.common.menu

import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.ITextDisplayEntity
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

const val MAX_TIMER_MINUTES: Long = 360

fun MenuComponent.registerTimer(
    position: Vector3d,
    timerMinutesProp: MutableProperty<Duration?>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    var timerMinutes by timerMinutesProp

    registerButton(
        position = position + Vector3d(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = Text.literal("+"),
        permissionGetter = permissionGetter,
    ) {
        val mins = timerMinutes?.inWholeMinutes ?: 0
        val newMins = if (mins < MAX_TIMER_MINUTES)
            ((mins / 30) + 1) * 30
        else null
        timerMinutes = newMins?.minutes
    }

    registerButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = Text.literal("-"),
        permissionGetter = permissionGetter,
    ) {
        val mins = timerMinutes?.inWholeMinutes ?: (MAX_TIMER_MINUTES + 30)
        val newMins = if (mins > 30)
            ((mins / 30) - 1) * 30
        else null
        timerMinutes = newMins?.minutes
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)

        val timerText = timerMinutes
            ?.inWholeMinutes
            ?.let { minutes ->
                String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
                    .let { Text.literal(it) }
            }
            ?: text.string(StringKey.OptionsTimeLimitOff)

        this.value = timerText
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsTimeLimit)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        lineWidth = 50
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}
