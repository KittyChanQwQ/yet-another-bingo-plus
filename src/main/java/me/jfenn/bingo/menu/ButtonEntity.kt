package me.jfenn.bingo.menu

import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.utils.EventListener
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class ButtonEntity(
    world: World = lobbyWorld,
    val position: Vec3d,
    text: String,
    isActive: Boolean = false,
    requiredPermissionLevel: Int = 2,
) {

    private val interactionListenerEntity = InteractionEntity(EntityType.INTERACTION, world)
        .apply {
            readNbt(NbtCompound().apply {
                put("Pos", position.add(0.0, 0.0, -0.5).toNbt())
                putFloat("width", 1f)
                putFloat("height", 0.25f)
            })
        }

    private val textDisplayEntity = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)

    val onClick = EventListener<PlayerEntity>()

    var text: String = text
        set(value) {
            field = value
            update()
        }

    var isActive: Boolean = isActive
        set(value) {
            field = value
            update()
        }

    init {
        update()
        world.spawnEntity(interactionListenerEntity)
        world.spawnEntity(textDisplayEntity)

        InteractionEntityEvents.onInteract(interactionListenerEntity) { player ->
            if (player.hasPermissionLevel(requiredPermissionLevel)) {
                onClick(player)
                interactionListenerEntity.playSound(SoundEvents.BLOCK_LEVER_CLICK, 1f, 1f)
            }
        }
    }

    private fun update() {
        textDisplayEntity.readNbt(NbtCompound().apply {
            put("Pos", position.toNbt())
            putString("text", Text.Serializer.toJson(
                Text.empty()
                    .append(if (isActive) Text.literal("✔ ") else Text.empty())
                    .append(Text.literal(text))
                    .formatted(if (isActive) Formatting.BLACK else Formatting.GRAY)
            ))
            putString("billboard", "fixed")
            putString("alignment", "center")
            putInt("background", (if (isActive) 0xA0_FFFFFFu else 0x00_000000u).toInt())
        })
    }

}