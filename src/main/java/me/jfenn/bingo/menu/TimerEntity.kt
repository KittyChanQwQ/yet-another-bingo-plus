package me.jfenn.bingo.menu

import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.utils.EventListener
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.time.Duration
import java.util.*

class TimerEntity(
    private val world: World = lobbyWorld,
    private val position: Vec3d,
    timerMinutesDefault: Int?,
    private val requiredPermissionLevel: Int = 2,
    private val title: String = "Time Limit",
    private val tags: Set<String> = emptySet(),
) {

    companion object {
        const val MAX_TIMER_MINUTES = 360
    }

    var timerMinutes: Int? = timerMinutesDefault
        set(value) {
            field = value
            onChange(value)
            update()
        }

    val onChange = EventListener<Int?>()

    val incrementButton = ButtonEntity(
        world = world,
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        requiredPermissionLevel = requiredPermissionLevel,
        tags = tags,
    ).apply {
        onClick {
            timerMinutes = (timerMinutes ?: 0).let { mins ->
                if (mins < MAX_TIMER_MINUTES)
                    ((mins / 30) + 1) * 30
                else null
            }
        }
    }

    val decrementButton = ButtonEntity(
        world = world,
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        requiredPermissionLevel = requiredPermissionLevel,
        tags = tags,
    ).apply {
        onClick {
            timerMinutes = (timerMinutes ?: (MAX_TIMER_MINUTES + 30)).let { mins ->
                if (mins > 30)
                    ((mins / 30) - 1) * 30
                else null
            }
        }
    }

    val textEntity = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
        .also { world.spawnEntity(it) }

    init {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal(title)
                    ))
                    putString("billboard", "fixed")
                    putString("alignment", "center")
                    putInt("background", 0)
                    putBoolean("shadow", true)
                    put("Tags", NbtList().apply {
                        tags.forEach { add(NbtString.of(it)) }
                    })
                })
            }
            .also { world.spawnEntity(it) }

        update()
    }

    private fun update() {
        val timerText = timerMinutes?.let {
            val duration = Duration.ofMinutes(it.toLong())
            String.format(Locale.US, "%dh %02dm", duration.toHoursPart(), duration.toMinutesPart())
        } ?: "Off"

        textEntity.readNbt(NbtCompound().apply {
            put("Pos", position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0).toNbt())
            putString("text", Text.Serializer.toJson(
                Text.literal(timerText)
            ))
            putString("billboard", "fixed")
            putString("alignment", "center")
            putInt("background", 0)
            putBoolean("shadow", true)
            put("Tags", NbtList().apply {
                tags.forEach { add(NbtString.of(it)) }
            })
        })
    }

}