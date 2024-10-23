package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.menu.tooltips.TooltipService
import me.jfenn.bingo.common.ready.ReadyTimerState
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerButton(
    position: Vector3d,
    text: Text,
    textProp: Property<Text> = computedProperty { text },
    tooltip: List<Text>? = null,
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    width: Float = (text.string.length * 0.15f).coerceIn(MENU_HITBOX_WIDTH, 2f),
    height: Float = MENU_HITBOX_HEIGHT,
    facing: Vector3d = Vector3d(0.0, 0.0, 1.0),
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    interactionEntityEvents: InteractionEntityEvents = koinScope.get(),
    playerManager: IPlayerManager = koinScope.get(),
    tooltipService: TooltipService = koinScope.get(),
    readyTimerState: ReadyTimerState = koinScope.get(),
    onClick: (player: IPlayerHandle) -> Unit,
) {
    val isActive by isActiveProp
    val buttonText by textProp

    val offset = -width / 2.0
    registerEntity(EntityType.INTERACTION) {
        pos = position.add(facing.x * offset, 0.0, facing.z * offset, Vector3d())
        this.width = width
        this.height = height
    }.onUpdate { entity ->
        if (tooltip != null) {
            tooltipService[entity] = tooltip
        }

        interactionEntityEvents.onInteract(entity) { player ->
            if (permissionGetter(player)) {
                onClick(playerManager.forPlayer(player))
                Sounds.playButtonSound(playerManager, player, entity.pos, player.serverWorld)
                // Reset the ready state, assuming an option has been changed
                readyTimerState.reset()
            }
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        pitch = 0f
        yaw = 0f
        this.value = Text.empty()
            .append(buttonText)
            .formatted(if (isActive) Formatting.BLACK else Formatting.GRAY)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = (if (isActive) 0xA0_FFFFFFu else 0x00_000000u).toInt()
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}

fun MenuComponent.registerToggleButton(
    position: Vector3d,
    text: Text,
    textProp: Property<Text?> = computedProperty { text },
    tooltip: List<Text>? = null,
    toggleProp: MutableProperty<Boolean>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    onClick: (player: IPlayerHandle) -> Unit = {},
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
        tooltip = tooltip,
        isActiveProp = toggleProp,
        permissionGetter = permissionGetter,
    ) {
        var toggle by toggleProp
        toggle = !toggle
        onClick(it)
    }
}
