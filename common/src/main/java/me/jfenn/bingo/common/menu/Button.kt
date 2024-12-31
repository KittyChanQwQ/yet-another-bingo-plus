package me.jfenn.bingo.common.menu

import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.menu.tooltips.TooltipService
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.platform.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerButtonInteraction(
    position: Vector3d,
    tooltip: List<Text>? = null,
    width: Float = 1f,
    height: Float = 1f,
    playerManager: IPlayerManager = koinScope.get(),
    permissions: PermissionsIntegration = koinScope.get(),
    permissionGetter: ((ServerPlayerEntity) -> Boolean)? = null,
    interactionEntityEvents: InteractionEntityEvents = koinScope.get(),
    tooltipService: TooltipService = koinScope.get(),
    events: ScopedEvents = koinScope.get(),
    executors: IExecutors = koinScope.get(),
    onClick: (player: IPlayerHandle) -> Unit,
) {
    val actualPermissionGetter = permissionGetter ?: {
        permissions.hasPermission(playerManager.forPlayer(it), Permission.CONFIGURE_GAME)
    }

    val offset = -width / 2.0
    registerEntity(EntityType.INTERACTION) {
        pos = position.add(0.0, 0.0, offset, Vector3d())
        this.width = width
        this.height = height
    }.onUpdate { entity ->
        if (tooltip != null) {
            tooltipService[entity] = tooltip
        }

        interactionEntityEvents.onInteract(entity) { player ->
            if (actualPermissionGetter(player)) {
                onClick(playerManager.forPlayer(player))
                Sounds.playButtonSound(playerManager, player, entity.pos, player.serverWorld)
                // Schedule an options changed event
                // (this clears the ready state & redraws the menu)
                executors.createServerTaskExecutor(player.server).execute {
                    events.onChangeOptions.invoke(Unit)
                }
            }
        }
    }
}

@Deprecated("Old code")
fun MenuComponent.registerButton(
    position: Vector3d,
    text: Text,
    textProp: Property<Text> = computedProperty { text },
    tooltip: List<Text>? = null,
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    width: Float = (text.string.length * 0.15f).coerceIn(MENU_HITBOX_WIDTH, 2f),
    height: Float = MENU_HITBOX_HEIGHT,
    permissionGetter: ((ServerPlayerEntity) -> Boolean)? = null,
    onClick: (player: IPlayerHandle) -> Unit,
) {
    val isActive by isActiveProp
    val buttonText by textProp

    registerButtonInteraction(
        position = position + Vector3d(0.0, -height/2.0, 0.0),
        tooltip = tooltip,
        width = width,
        height = height,
        permissionGetter = permissionGetter,
        onClick = onClick,
    )

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

fun MenuComponent.registerTileButton(
    position: Vector3d,
    text: Text? = null,
    textProp: Property<Text> = computedProperty { text ?: Text.empty() },
    tooltip: List<Text>? = null,
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    blockIdentifier: String = MENU_BUTTON_MATERIAL,
    width: Double = 1.0,
    height: Double = 1.0,
    permissionGetter: ((ServerPlayerEntity) -> Boolean)? = null,
    onClick: (player: IPlayerHandle) -> Unit,
) {
    val isActive by isActiveProp
    val buttonText by textProp
    val buttonTextColor by computedProperty { if (isActive) Formatting.BLACK else Formatting.GRAY }
    val isVisible by computedProperty { text == null || buttonText != Text.empty() }

    registerButtonInteraction(
        position = position,
        tooltip = tooltip,
        width = width.toFloat(),
        height = height.toFloat(),
        permissionGetter = permissionGetter,
        onClick = onClick,
    )

    registerEntity(EntityType.BLOCK_DISPLAY) {
        pos = position + Vector3d(-width/2, 0.0, -0.051)
        this.blockIdentifier = if (isVisible) blockIdentifier else "minecraft:air"
        brightness = IDisplayEntity.Brightness(0, if (isActive) 15 else 3)
        transformation = Matrix4f().scale(width.toFloat(), height.toFloat(), 0.05f)
    }

    if (isActiveProp !is ConstantProperty) {
        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = position + Vector3d(-width/2 + 0.15, height/2 - 0.1, 0.005)
            pitch = 0f
            yaw = 0f
            this.value = Text.literal("◇").formatted(buttonTextColor)
            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            transformation = Matrix4f().scale(MENU_TEXT_SCALE)
        }

        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = position + Vector3d(-width/2 + 0.15, height/2 - 0.1, 0.0)
            pitch = 0f
            yaw = 0f
            this.value = Text.literal(
                if (isActive) "◆" else ""
            ).formatted(Formatting.DARK_GREEN)
            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            transformation = Matrix4f().scale(MENU_TEXT_SCALE)
        }
    }

    if (text != null) {
        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = position + Vector3d(0.0, height/2 - 0.1, 0.0)
            pitch = 0f
            yaw = 0f
            this.value = Text.empty().append(buttonText).formatted(buttonTextColor)
            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            transformation = Matrix4f().scale(MENU_TEXT_SCALE)
        }
    }
}

fun MenuComponent.registerIconButton(
    position: Vector3d,
    icon: String,
    text: Text,
    textProp: Property<Text> = computedProperty { text },
    tooltip: List<Text>? = null,
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    width: Double = 0.6,
    height: Double = 0.6,
    permissionGetter: ((ServerPlayerEntity) -> Boolean)? = null,
    onClick: (player: IPlayerHandle) -> Unit,
) {
    val isActive by isActiveProp
    val buttonText by textProp
    val buttonTextColor by computedProperty { if (isActive) Formatting.BLACK else Formatting.GRAY }

    registerTileButton(
        position = position,
        tooltip = tooltip,
        isActiveProp = isActiveProp,
        width = width,
        height = height,
        permissionGetter = permissionGetter,
        onClick = onClick,
    )

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position + Vector3d(0.0, height/2 - 0.15, 0.0)
        pitch = 0f
        yaw = 0f
        this.value = Text.literal(icon).formatted(buttonTextColor)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        transformation = Matrix4f().scale(1.6f)
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position + Vector3d(0.0, height/2 - 0.25, 0.0)
        pitch = 0f
        yaw = 0f
        this.value = Text.empty()
            .append(buttonText)
            .formatted(buttonTextColor)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        transformation = Matrix4f().scale(0.4f)
        lineWidth = 70
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
