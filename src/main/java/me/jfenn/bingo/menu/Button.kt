package me.jfenn.bingo.menu

import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerButton(
    position: Vec3d,
    text: String,
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    width: Float = MENU_HITBOX_WIDTH,
    height: Float = MENU_HITBOX_HEIGHT,
    facing: Direction = Direction.SOUTH,
    requiredPermissionLevel: Int = 2,
    onClick: (player: PlayerEntity) -> Unit,
) {
    val isActive by isActiveProp

    registerEntity(EntityType.INTERACTION) {
        val offset = -width / 2.0
        put("Pos", position.add(facing.vector.x * offset, 0.0, facing.vector.z * offset).toNbt())
        putFloat("width", width)
        putFloat("height", height)
    }.onUpdate { entity ->
        InteractionEntityEvents.onInteract(entity) { player ->
            if (player.hasPermissionLevel(requiredPermissionLevel)) {
                onClick(player)
                entity.playSound(SoundEvents.BLOCK_LEVER_CLICK, 1f, 1f)
            }
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.toNbt())
        put("Rotation", NbtList().apply {
            add(NbtFloat.of(facing.asRotation()))
            add(NbtFloat.of(0f))
        })
        putString("text", Text.Serializer.toJson(
            Text.empty()
                .append(if (isActive) Text.literal("✔ ") else Text.empty())
                .append(Text.literal(text))
                .formatted(if (isActive) Formatting.BLACK else Formatting.GRAY)
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", (if (isActive) 0xA0_FFFFFFu else 0x00_000000u).toInt())
    }
}

fun MenuComponent.registerToggleButton(
    position: Vec3d,
    text: String,
    toggleProp: MutableProperty<Boolean>,
) {
    registerButton(
        position = position,
        text = text,
        isActiveProp = toggleProp,
    ) {
        var toggle by toggleProp
        toggle = !toggle
    }
}
