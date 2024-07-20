package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f

fun MenuComponent.registerButton(
    position: Vec3d,
    text: Text,
    textProp: Property<Text> = computedProperty { text },
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    width: Float = (text.string.length * 0.15f).coerceIn(MENU_HITBOX_WIDTH, 2f),
    height: Float = MENU_HITBOX_HEIGHT,
    facing: Direction = Direction.SOUTH,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    interactionEntityEvents: InteractionEntityEvents = koinScope.get(),
    textProvider: TextProvider = koinScope.get(),
    playerManager: IPlayerManager = koinScope.get(),
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
                Sounds.playButtonSound(playerManager, player, entity.pos, player.serverWorld)
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
        put("transformation", Matrix4f().scale(MENU_TEXT_SCALE).toNbt())
    }
}

fun MenuComponent.registerToggleButton(
    position: Vec3d,
    text: Text,
    textProp: Property<Text?> = computedProperty { text },
    toggleProp: MutableProperty<Boolean>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    onClick: (player: ServerPlayerEntity) -> Unit = {},
) {
    val isActive by toggleProp
    val buttonText by textProp

    registerButton(
        position = position,
        text = text,
        textProp = computedProperty {
            buttonText?.let {
                Text.empty()
                    .append(if (isActive) "✔ " else "✗ ")
                    .append(it)
            } ?: Text.empty()
        },
        isActiveProp = toggleProp,
        permissionGetter = permissionGetter,
    ) {
        var toggle by toggleProp
        toggle = !toggle
        onClick(it)
    }
}
