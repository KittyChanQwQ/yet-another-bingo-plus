package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.text.TextProvider
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.max

fun MenuComponent.registerButton(
    position: Vec3d,
    text: Text,
    textProp: Property<Text> = computedProperty { text },
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    width: Float = max(MENU_HITBOX_WIDTH, text.string.length * 0.15f),
    height: Float = MENU_HITBOX_HEIGHT,
    facing: Direction = Direction.SOUTH,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    interactionEntityEvents: InteractionEntityEvents = koinScope.get(),
    textProvider: TextProvider = koinScope.get(),
    onClick: (player: ServerPlayerEntity) -> Unit,
) {
    val isActive by isActiveProp
    val buttonText by textProp

    val offset = -width / 2.0
    registerEntity(EntityType.INTERACTION, position.add(facing.vector.x * offset, 0.0, facing.vector.z * offset)) {
        putFloat("width", width)
        putFloat("height", height)
    }.onUpdate { entity ->
        interactionEntityEvents.onInteract(entity) { player ->
            if (permissionGetter(player)) {
                onClick(player)
                Sounds.playButtonSound(player, entity.pos)
            }
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        put("Rotation", NbtList().apply {
            add(NbtFloat.of(facing.asRotation()))
            add(NbtFloat.of(0f))
        })
        putString("text", textProvider.serialized(
            Text.empty()
                .append(buttonText)
                .formatted(if (isActive) Formatting.BLACK else Formatting.GRAY)
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", (if (isActive) 0xA0_FFFFFFu else 0x00_000000u).toInt())
    }
}

fun MenuComponent.registerToggleButton(
    position: Vec3d,
    text: Text,
    toggleProp: MutableProperty<Boolean>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    onClick: (player: ServerPlayerEntity) -> Unit = {},
) {
    val isActive by toggleProp

    registerButton(
        position = position,
        text = text,
        textProp = computedProperty {
            Text.empty()
                .append(if (isActive) "✔ " else "✗ ")
                .append(text)
        },
        isActiveProp = toggleProp,
        permissionGetter = permissionGetter,
    ) {
        var toggle by toggleProp
        toggle = !toggle
        onClick(it)
    }
}
