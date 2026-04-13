package me.jfenn.bingo.impl

import com.mojang.authlib.GameProfile
import me.jfenn.bingo.api.annotations.BingoInternalApi
import me.jfenn.bingo.api.internal.ExtensionManager
import me.jfenn.bingo.impl.inventory.InventoryFactoryImpl
import me.jfenn.bingo.impl.inventory.InventoryImpl
import me.jfenn.bingo.impl.inventory.PlayerScreenInventoryImpl
import me.jfenn.bingo.impl.player.ContainerMenuImpl
import me.jfenn.bingo.platform.*
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.commands.ISignedMessage
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.player.IContainerMenu
import me.jfenn.bingo.platform.player.PlayerProfile
import me.jfenn.bingo.platform.scope.BingoKoin
import me.jfenn.bingo.platform.text.IText
import net.minecraft.commands.CommandSource
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.OutgoingChatMessage
import net.minecraft.network.protocol.game.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.*
import net.minecraft.world.level.GameType
import net.minecraft.world.level.storage.LevelData
import net.minecraft.world.phys.HitResult
import org.joml.Vector3d
import java.util.*

class PlayerManager(
    private val server: MinecraftServer,
    private val itemStackFactory: IItemStackFactory,
) : IPlayerManager {
    override fun forPlayer(player: ServerPlayer): IPlayerHandle {
        return PlayerHandle(player, itemStackFactory)
    }

    override fun getPlayer(uuid: UUID): IPlayerHandle? {
        return server.playerList.getPlayer(uuid)
            ?.let { PlayerHandle(it, itemStackFactory) }
    }

    override fun getPlayers(): List<IPlayerHandle> {
        return server.playerList.players
            .map { PlayerHandle(it, itemStackFactory) }
    }

    override fun updatePlayerListName(player: IPlayerHandle) {
        val packet = ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player.player)
        server.playerList.broadcastAll(packet)
    }

    override fun getOfflinePlayer(profile: PlayerProfile): IPlayerHandle {
        val player = ServerPlayer(
            server,
            server.overworld(),
            GameProfile(profile.uuid, profile.name),
            ClientInformation.createDefault(),
        )

        server.playerList.loadPlayerData(player.nameAndId())
        return PlayerHandle(player, itemStackFactory)
    }

    override fun playToAround(
        player: ServerPlayer?,
        sound: PlayerSoundEvent,
        category: PlayerSoundCategory,
        volume: Float,
        pitch: Float,
        position: Triple<Double, Double, Double>,
        world: IServerWorld,
    ) {
        val (x, y, z) = position
        val soundEntry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.toSoundEvent())
        val packet = ClientboundSoundPacket(soundEntry, category.toSoundCategory(), x, y, z, volume, pitch, server.overworld().seed)
        player?.connection?.send(packet)
        server.playerList.broadcast(player, x, y, z, soundEntry.value().getRange(volume).toDouble(), world.world.dimension(), packet)
    }

    override fun broadcastChatMessage(message: ISignedMessage, sender: IPlayerHandle) {
        require(message is SignedMessageImpl)
        require(sender is PlayerHandle)

        val parameters: ChatType.Bound = ChatType.bind(ChatType.CHAT, sender.player)
        server.playerList.broadcastChatMessage(message.message, sender.player, parameters)
    }
}

class PlayerHandle(
    override val player: ServerPlayer,
    private val itemStackFactory: IItemStackFactory? = null,
) : IPlayerHandle, LivingEntityImpl(player) {

    private fun getItemStackFactory() = itemStackFactory
        ?: BingoKoin.getScope(player.level().server)!!.get<IItemStackFactory>()

    override val world: IServerWorld
        get() = ServerWorldImpl(player.level(), getItemStackFactory())

    override val server: IMinecraftServer
        get() = MinecraftServerImpl(requireNotNull(player.level().server) { "Player $player entityWorld.server reference is null!" })

    override val isAlive: Boolean
        get() = player.isAlive
    override val isSpectator: Boolean
        get() = player.isSpectator

    override val playerName: String
        get() = player.scoreboardName

    override val commandSource: CommandSource
        get() = player.commandSource()

    override var fireTicks: Int by player::remainingFireTicks
    override var isOnFire: Boolean
        get() = player.isOnFire
        set(value) {
            player.setSharedFlagOnFire(value)
        }

    override var foodLevel: Int by player.foodData::foodLevel
    override var saturationLevel: Float
        get() = player.foodData.saturationLevel
        set(value) { player.foodData.setSaturation(value) }
    override var exhaustion: Float by player.foodData.accessor::exhaustion

    override var experienceLevel: Int
        get() = player.experienceLevel
        set(value) { player.setExperienceLevels(value) }

    override var experienceProgress: Float
        get() = player.experienceProgress
        set(value) { player.setExperiencePoints((value * player.xpNeededForNextLevel).toInt()) }

    override val isSneaking: Boolean
        get() = player.isShiftKeyDown

    override val itemUseTimeLeft: Int
        get() = player.useItemRemainingTicks

    override fun sendMessage(message: IText) {
        player.sendSystemMessage(message.value)
    }

    override fun sendHotbarMessage(message: IText) {
        player.sendSystemMessage(message.value, true)
    }

    override fun sendChatMessage(message: ISignedMessage, sender: IPlayerHandle) {
        require(message is SignedMessageImpl)
        require(sender is PlayerHandle)

        val parameters: ChatType.Bound = ChatType.bind(ChatType.CHAT, sender.player)
        player.sendChatMessage(OutgoingChatMessage.create(message.message), false, parameters)
    }

    override fun sendTeamMessage(message: ISignedMessage, sender: IPlayerHandle, teamName: IText) {
        require(message is SignedMessageImpl)
        require(sender is PlayerHandle)

        val parameters: ChatType.Bound = ChatType.bind(
            if (sender.uuid == this.uuid) ChatType.TEAM_MSG_COMMAND_OUTGOING else ChatType.TEAM_MSG_COMMAND_INCOMING,
            sender.player
        ).withTargetName(teamName.value)
        player.sendChatMessage(OutgoingChatMessage.create(message.message), false, parameters)
    }

    override fun sendTeamMessage(message: IText, sender: IPlayerHandle, teamName: IText) {
        require(sender is PlayerHandle)

        val parameters: ChatType.Bound = ChatType.bind(
            if (sender.uuid == this.uuid) ChatType.TEAM_MSG_COMMAND_OUTGOING else ChatType.TEAM_MSG_COMMAND_INCOMING,
            sender.player
        ).withTargetName(teamName.value)
        val decoratedMessage = parameters.chatType.value().chat.decorate(message.value, parameters)

        player.sendSystemMessage(decoratedMessage)
    }

    override fun sendTitle(title: IText, subtitle: IText?) {
        player.connection.send(ClientboundSetTitleTextPacket(title.value))
        player.connection.send(ClientboundSetSubtitleTextPacket(subtitle?.value ?: Component.empty()))
    }

    override fun openMenu(menu: IContainerMenu) {
        require(menu is ContainerMenuImpl)
        player.openMenu(menu.menuProvider)
    }

    override fun hasPermissionLevel(level: Int): Boolean {
        val permissions = when (level) {
            0 -> return true
            1 -> Permissions.COMMANDS_MODERATOR
            2 -> Permissions.COMMANDS_GAMEMASTER
            3 -> Permissions.COMMANDS_ADMIN
            else -> Permissions.COMMANDS_OWNER
        }
        return player.permissions().hasPermission(permissions)
    }

    override fun canUseItem(stack: IItemStack): Boolean {
        val itemStack: ItemStack = stack.stack
        return when {
            itemStack.isEmpty -> false
            itemStack.useAnimation == ItemUseAnimation.EAT -> player.canEat(false)
            itemStack.useAnimation == ItemUseAnimation.DRINK -> true
            itemStack.useAnimation == ItemUseAnimation.BLOCK -> true // blocking with a shield
            itemStack.item is BoatItem -> true // trying to place a boat
            itemStack.item is BlockItem -> {
                // if holding a block, check if it can be placed on a surface within reach
                val hit = player.pick(5.0, 0f, false)
                hit.type == HitResult.Type.BLOCK
            }
            itemStack.item == Items.FIREWORK_ROCKET -> player.isFallFlying || player.isInWater
            itemStack.item == Items.SUSPICIOUS_STEW -> true
            itemStack.useOnRelease() -> true
            itemStack.useAnimation == ItemUseAnimation.NONE -> false
            else -> true
        }
    }

    override var gameMode: PlayerGameMode
        get() = player.gameMode.gameModeForPlayer.toGameMode()
        set(value) {
            player.setGameMode(value.toGameMode())
        }

    override val mainHandStack: IItemStack
        get() = getItemStackFactory().forStack(player.mainHandItem)

    override val offHandStack: IItemStack
        get() = getItemStackFactory().forStack(player.offhandItem)

    override val inventory: IInventory
        get() = InventoryImpl(getItemStackFactory(), player.inventory)

    override val screenInventory: IInventory
        get() = PlayerScreenInventoryImpl(getItemStackFactory(), player)

    override val enderChestInventory: IInventory
        get() = InventoryImpl(getItemStackFactory(), player.enderChestInventory)

    @OptIn(BingoInternalApi::class)
    override val extraInventories: List<IInventory>
        get() {
            val inventoryFactory = InventoryFactoryImpl(getItemStackFactory())
            return ExtensionManager.inventoryProviders
                .flatMap { it.getInventories(player) }
                .map { inventoryFactory.create(it) }
        }

    override fun giveOrEquipStack(stack: IItemStack) {
        val itemStack: ItemStack = stack.stack
        val equippable = itemStack[DataComponents.EQUIPPABLE]

        // if the item can be equipped (e.g. iron chestplate), put it in an equipment slot
        val slot = equippable?.slot
        if (slot != null && player.isEquippableInSlot(itemStack, slot) && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            player.setItemSlot(slot, itemStack)
        } else {
            // otherwise, put it in the player's inventory
            player.addItem(itemStack)
        }
    }

    override fun giveItemStack(stack: IItemStack) {
        player.addItem(stack.stack)
    }

    override fun removeStack(slot: Int) {
        player.inventory.removeItemNoUpdate(slot)
    }

    override fun setStack(slot: Int, stack: IItemStack) {
        if (slot in 0 until Inventory.INVENTORY_SIZE) {
            player.getSlot(slot)?.set(stack.stack)
        } else {
            player.inventory.setItem(slot, stack.stack)
        }
    }

    override fun playSound(sound: PlayerSoundEvent, category: PlayerSoundCategory, volume: Float, pitch: Float) {
        val soundEntry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.toSoundEvent())
        val packet = ClientboundSoundPacket(
            soundEntry,
            category.toSoundCategory(),
            player.x,
            player.y,
            player.z,
            volume,
            pitch,
            0L,
        )
        player.connection.send(packet)
    }

    override val serverWorld: ServerLevel get() = player.level()

    override val blockPos: BlockPosition
        get() = BlockPosition(player.blockX, player.blockY, player.blockZ)

    override val eyePos: Vector3d
        get() = Vector3d(player.eyePosition.x, player.eyePosition.y, player.eyePosition.z)

    override fun respawn(): IPlayerHandle {
        val networkHandler = player.connection
        networkHandler.handleClientCommand(ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN))
        return PlayerHandle(networkHandler.player, itemStackFactory)
    }

    override fun teleport(world: IServerWorld, pos: Vector3d, yaw: Float, pitch: Float) {
        player.teleportTo(world.world, pos.x, pos.y, pos.z, emptySet(), yaw, pitch, true)
    }

    override fun setSpawnPoint(
        world: IServerWorld,
        spawn: BlockPosition,
        angle: Float,
        forced: Boolean,
        sendMessage: Boolean
    ) {
        val serverWorld: ServerLevel = world.world
        val respawnData = LevelData.RespawnData.of(
            serverWorld.dimension(),
            spawn.toBlockPos(),
            angle,
            0f,
        )
        player.setRespawnPosition(
            ServerPlayer.RespawnConfig(respawnData, forced),
            sendMessage
        )
    }

    override fun startRiding(entity: IEntity, force: Boolean) {
        player.startRiding(entity.entity, true, false)
    }

    override val abilities: IPlayerAbilities
        get() = object : IPlayerAbilities {
            override var allowFlying by player.abilities::mayfly
        }

    override fun sendAbilitiesUpdate() {
        player.onUpdateAbilities()
    }
}

private fun PlayerSoundEvent.toSoundEvent() = when (this) {
    PlayerSoundEvent.BLOCK_LEVER_CLICK -> SoundEvents.LEVER_CLICK
    PlayerSoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON -> SoundEvents.WOODEN_BUTTON_CLICK_ON
    PlayerSoundEvent.BLOCK_WOODEN_BUTTON_CLICK_OFF -> SoundEvents.WOODEN_BUTTON_CLICK_OFF
    PlayerSoundEvent.ENTITY_PLAYER_LEVELUP -> SoundEvents.PLAYER_LEVELUP
    PlayerSoundEvent.ENTITY_SHULKER_AMBIENT -> SoundEvents.SHULKER_AMBIENT
    PlayerSoundEvent.ENTITY_TNT_PRIMED -> SoundEvents.TNT_PRIMED
    PlayerSoundEvent.BLOCK_NOTE_BLOCK_BASS -> SoundEvents.NOTE_BLOCK_BASS.value()
    PlayerSoundEvent.ITEM_LODESTONE_COMPASS_LOCK -> SoundEvents.LODESTONE_COMPASS_LOCK
    PlayerSoundEvent.BLOCK_PORTAL_TRAVEL -> SoundEvents.PORTAL_TRAVEL
    PlayerSoundEvent.BLOCK_NOTE_BLOCK_PLING -> SoundEvents.NOTE_BLOCK_PLING.value()
}

private fun PlayerSoundCategory.toSoundCategory() = when (this) {
    PlayerSoundCategory.MAIN -> SoundSource.MASTER
    PlayerSoundCategory.RECORDS -> SoundSource.RECORDS
    PlayerSoundCategory.BLOCKS -> SoundSource.BLOCKS
}

private fun GameType.toGameMode() = when (this) {
    GameType.SURVIVAL -> PlayerGameMode.SURVIVAL
    GameType.CREATIVE -> PlayerGameMode.CREATIVE
    GameType.ADVENTURE -> PlayerGameMode.ADVENTURE
    GameType.SPECTATOR -> PlayerGameMode.SPECTATOR
}

private fun PlayerGameMode.toGameMode() = when (this) {
    PlayerGameMode.SURVIVAL -> GameType.SURVIVAL
    PlayerGameMode.CREATIVE -> GameType.CREATIVE
    PlayerGameMode.ADVENTURE -> GameType.ADVENTURE
    PlayerGameMode.SPECTATOR -> GameType.SPECTATOR
}