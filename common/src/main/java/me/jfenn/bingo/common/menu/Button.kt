package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.utils.hasPermission
import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
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
    onClick: (player: ServerPlayerEntity) -> Unit,
) {
    val isActive by isActiveProp

    val offset = -width / 2.0
    registerEntity(EntityType.INTERACTION, position.add(facing.vector.x * offset, 0.0, facing.vector.z * offset)) {
        putFloat("width", width)
        putFloat("height", height)
    }.onUpdate { entity ->
        InteractionEntityEvents.onInteract(entity) { player ->
            if (player.hasPermission(requiredPermissionLevel)) {
                onClick(player)
                entity.playSound(SoundEvents.BLOCK_LEVER_CLICK, 1f, 1f)
            }
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        put("Rotation", NbtList().apply {
            add(NbtFloat.of(facing.asRotation()))
            add(NbtFloat.of(0f))
        })
        putString("text", TextInfo.toJsonString(
            TextInfo.empty()
                .append(if (isActive) TextInfo.literal("✔ ") else TextInfo.empty())
                .append(TextInfo.literal(text))
                .formatted(if (isActive) TextColor.BLACK else TextColor.GRAY)
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
