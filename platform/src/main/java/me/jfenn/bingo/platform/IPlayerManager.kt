package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.commands.ISignedMessage
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.player.IContainerMenu
import me.jfenn.bingo.platform.player.PlayerProfile
import me.jfenn.bingo.platform.text.IText
import net.minecraft.commands.CommandSource
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import org.joml.Vector3d
import java.util.*

interface IPlayerManager {

    fun forPlayer(player: ServerPlayer): IPlayerHandle

    fun getPlayer(uuid: UUID): IPlayerHandle?

    fun getPlayers(): List<IPlayerHandle>

    fun getOfflinePlayer(profile: PlayerProfile): IPlayerHandle

    fun updatePlayerListName(player: IPlayerHandle)

    /**
     * Sends a sound manually to [player], and broadcasts a
     * sound event to surrounding players.
     *
     * If lobbyChaosProtection=true, this will result in
     * only [player] hearing the sound.
     */
    fun playToAround(player: ServerPlayer?, sound: PlayerSoundEvent, category: PlayerSoundCategory, volume: Float, pitch: Float, position: Triple<Double, Double, Double>, world: IServerWorld)

    fun broadcastChatMessage(message: ISignedMessage, sender: IPlayerHandle)

}

interface IPlayerHandle : ILivingEntity {
    val player: ServerPlayer
    val server: IMinecraftServer?
    val world: IServerWorld
    val isAlive: Boolean
    val isSpectator: Boolean
    val playerName: String
    val commandSource: CommandSource

    var fireTicks: Int
    var isOnFire: Boolean
    var foodLevel: Int
    val maxFoodLevel: Int get() = 20
    var saturationLevel: Float
    val maxSaturationLevel: Float get() = 5f
    var exhaustion: Float
    val isSneaking: Boolean
    var experienceLevel: Int
    var experienceProgress: Float
    val itemUseTimeLeft: Int

    val profile get() = PlayerProfile(uuid, playerName)

    /**
     * vaguely determine if the held item will be used or not
     * (e.g. suspicious stew, potions, etc. - vs. static items such as iron ingots)
     */
    fun canUseItem(stack: IItemStack): Boolean

    var gameMode: PlayerGameMode
    val mainHandStack: IItemStack
    val offHandStack: IItemStack

    fun getStackInHand(hand: Hand): IItemStack = when (hand) {
        Hand.MAIN_HAND -> mainHandStack
        Hand.OFF_HAND -> offHandStack
    }

    val inventory: IInventory
    val screenInventory: IInventory
    val enderChestInventory: IInventory
    val extraInventories: List<IInventory>
    val allInventories get() = listOf(inventory, screenInventory, enderChestInventory) + extraInventories

    /**
     * Give the player an ItemStack, equipping it in the preferred slot if armor,
     * and otherwise placing it in the inventory
     */
    fun giveOrEquipStack(stack: IItemStack)

    fun giveItemStack(stack: IItemStack)
    fun removeStack(slot: Int)
    fun setStack(slot: Int, stack: IItemStack)

    fun playSound(sound: PlayerSoundEvent, category: PlayerSoundCategory, volume: Float, pitch: Float)

    val serverWorld: Level
    val blockPos: BlockPosition
    val eyePos: Vector3d

    fun respawn(): IPlayerHandle
    fun teleport(world: IServerWorld, pos: Vector3d, yaw: Float, pitch: Float)
    fun setSpawnPoint(world: IServerWorld, spawn: BlockPosition, angle: Float, forced: Boolean, sendMessage: Boolean)

    fun hasPermissionLevel(level: Int): Boolean
    fun sendMessage(message: IText)
    fun sendHotbarMessage(message: IText)
    fun sendChatMessage(message: ISignedMessage, sender: IPlayerHandle)
    fun sendTeamMessage(message: ISignedMessage, sender: IPlayerHandle, teamName: IText)
    fun sendTeamMessage(message: IText, sender: IPlayerHandle, teamName: IText)

    fun sendTitle(title: IText, subtitle: IText?)
    fun openMenu(menu: IContainerMenu)

    fun startRiding(entity: IEntity, force: Boolean)

    val abilities: IPlayerAbilities
    fun sendAbilitiesUpdate()
}

interface IPlayerAbilities {
    var allowFlying: Boolean
}

enum class PlayerSoundEvent {
    BLOCK_LEVER_CLICK,
    BLOCK_WOODEN_BUTTON_CLICK_ON,
    BLOCK_WOODEN_BUTTON_CLICK_OFF,
    ENTITY_PLAYER_LEVELUP,
    ENTITY_SHULKER_AMBIENT,
    ENTITY_TNT_PRIMED,
    BLOCK_NOTE_BLOCK_BASS,
    ITEM_LODESTONE_COMPASS_LOCK,
    BLOCK_PORTAL_TRAVEL,
    BLOCK_NOTE_BLOCK_PLING,
}

enum class PlayerSoundCategory {
    MAIN,
    RECORDS,
    BLOCKS,
}

enum class PlayerGameMode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR
}

enum class Hand { MAIN_HAND, OFF_HAND }