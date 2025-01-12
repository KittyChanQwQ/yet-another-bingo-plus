package me.jfenn.bingo.common.menu

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
import org.slf4j.Logger

internal fun MenuComponent.registerButtonInteraction(
    position: Vector3d,
    tooltip: List<Text>? = null,
    tooltipProp: Property<List<Text>?> = ConstantProperty(tooltip),
    width: Float = 1f,
    height: Float = 1f,
    playerManager: IPlayerManager = koinScope.get(),
    permissions: PermissionsIntegration = koinScope.get(),
    permissionGetter: ((ServerPlayerEntity) -> Boolean)? = null,
    interactionEntityEvents: InteractionEntityEvents = koinScope.get(),
    tooltipService: TooltipService = koinScope.get(),
    events: ScopedEvents = koinScope.get(),
    executors: IExecutors = koinScope.get(),
    log: Logger = koinScope.get(),
    clickSound: (() -> PlayerSoundEvent)? = null,
    onClick: (player: IPlayerHandle) -> Unit,
) {
    val actualPermissionGetter = permissionGetter ?: {
        permissions.hasPermission(playerManager.forPlayer(it), Permission.CONFIGURE_GAME)
    }

    val offset = -width / 2.0
    val tooltipList by tooltipProp

    registerEntity(EntityType.INTERACTION) {
        pos = position.add(0.0, 0.0, offset, Vector3d())
        this.width = width
        this.height = height
    }.onUpdate { entity ->
        tooltipList?.let {
            tooltipService[entity] = it
        }

        interactionEntityEvents.onInteract(entity) { player ->
            if (actualPermissionGetter(player)) {
                try {
                    onClick(playerManager.forPlayer(player))
                } catch (e: Throwable) {
                    log.error("Exception during interaction onClick:", e)
                }

                Sounds.playButtonSound(
                    playerManager,
                    player,
                    entity.pos,
                    player.serverWorld,
                    clickSound?.invoke() ?: PlayerSoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON
                )
                // Schedule an options changed event
                // (this clears the ready state & redraws the menu)
                executors.createServerTaskExecutor(player.server).execute {
                    events.onChangeOptions.invoke(Unit)
                }
            }
        }
    }
}

internal fun MenuComponent.registerTileButton(
    position: Vector3d,
    icon: String? = null,
    text: Text? = null,
    textProp: Property<Text> = ConstantProperty(text ?: Text.empty()),
    textScale: Float = MENU_TEXT_SCALE,
    tooltip: List<Text>? = null,
    tooltipProp: Property<List<Text>?> = ConstantProperty(tooltip),
    isActiveProp: Property<Boolean> = ConstantProperty(false),
    blockIdentifier: String = MENU_BUTTON_MATERIAL,
    width: Double = 1.0,
    height: Double = MENU_LINE_HEIGHT,
    permissionGetter: ((ServerPlayerEntity) -> Boolean)? = null,
    onClick: (player: IPlayerHandle) -> Unit,
) {
    val isActive by isActiveProp
    val buttonText by textProp
    val buttonTextColor by computedProperty { if (isActive) Formatting.BLACK else Formatting.GRAY }
    val isVisible by computedProperty { text == null || buttonText != Text.empty() }

    registerButtonInteraction(
        position = position,
        tooltipProp = tooltipProp,
        width = width.toFloat(),
        height = height.toFloat(),
        permissionGetter = permissionGetter,
        clickSound = { if (isActive) PlayerSoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON else PlayerSoundEvent.BLOCK_WOODEN_BUTTON_CLICK_OFF },
        onClick = onClick,
    )

    registerEntity(EntityType.BLOCK_DISPLAY) {
        pos = position + Vector3d(-width/2, 0.0, -0.051)
        this.blockIdentifier = if (isVisible) blockIdentifier else "minecraft:air"
        brightness = if (isActive) MENU_BRIGHTNESS_ON else MENU_BRIGHTNESS_OFF
        transformation = Matrix4f().scale(width.toFloat(), height.toFloat(), 0.05f)
    }

    if (icon != null || isActiveProp !is ConstantProperty) {
        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = position + Vector3d(-width/2 + 0.15, height/2 - 0.1, 0.005)
            pitch = 0f
            yaw = 0f
            this.value = when {
                isVisible -> Text.literal(icon ?: "◇").formatted(buttonTextColor)
                else -> Text.empty()
            }
            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            transformation = Matrix4f().scale(MENU_TEXT_SCALE)
        }
    }

    if (icon == null && isActiveProp !is ConstantProperty) {
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
            pos = position + Vector3d(0.0, height/2.0 - textScale/7.0, 0.0)
            pitch = 0f
            yaw = 0f
            this.value = Text.empty().append(buttonText).formatted(buttonTextColor)
            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            transformation = Matrix4f().scale(textScale)
            lineWidth = (width * 30f / textScale).toInt()
        }
    }
}

internal fun MenuComponent.registerIconButton(
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
        lineWidth = (width * 65).toInt()
    }
}

internal fun MenuComponent.registerToggleButton(
    position: Vector3d,
    width: Double = 1.0,
    height: Double = MENU_LINE_HEIGHT,
    text: Text? = null,
    textProp: Property<Text> = computedProperty { text ?: Text.empty() },
    toggleProp: MutableProperty<Boolean>,
    tooltip: List<Text>? = null,
    onClick: (player: IPlayerHandle) -> Unit = {},
) {
    var toggle by toggleProp

    registerTileButton(
        position = position,
        width = width,
        height = height,
        text = text,
        textProp = textProp,
        tooltip = tooltip,
        isActiveProp = toggleProp
    ) {
        toggle = !toggle
        onClick(it)
    }
}
