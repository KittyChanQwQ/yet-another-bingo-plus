package me.jfenn.bingo.api

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.team.BingoPlayerProfile
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import org.joml.Vector3d
import java.util.*

interface IPlayerManager {

    fun forPlayer(player: ServerPlayerEntity): IPlayerHandle

    fun getPlayer(uuid: UUID): IPlayerHandle?

    fun getPlayers(): List<IPlayerHandle>

    fun getOfflinePlayer(profile: BingoPlayerProfile): IPlayerHandle

    /**
     * Sends a sound manually to [player], and broadcasts a
     * sound event to surrounding players.
     *
     * If lobbyChaosProtection=true, this will result in
     * only [player] hearing the sound.
     */
    fun playToAround(player: ServerPlayerEntity?, sound: PlayerSoundEvent, category: PlayerSoundCategory, volume: Float, pitch: Float, position: Triple<Double, Double, Double>, world: ServerWorld)

}

interface IPlayerHandle : ILivingEntity {
    val player: ServerPlayerEntity
    val isAlive: Boolean
    val isSpectator: Boolean
    val name: String

    var fireTicks: Int
    var isOnFire: Boolean
    var foodLevel: Int
    val maxFoodLevel: Int get() = 20
    var saturationLevel: Float
    val maxSaturationLevel: Float get() = 5f
    var exhaustion: Float

    fun allHeldStacks(): Sequence<IItemStack>
    fun allInventorySlots(): Sequence<Pair<Int, IItemStack>>

    /**
     * Give the player an ItemStack, equipping it in the preferred slot if armor,
     * and otherwise placing it in the inventory
     */
    fun giveOrEquipStack(stack: IItemStack)

    fun giveItemStack(stack: IItemStack)
    fun removeStack(slot: Int)
    fun setStack(slot: Int, stack: IItemStack)

    fun playSound(sound: PlayerSoundEvent, category: PlayerSoundCategory, volume: Float, pitch: Float)

    val serverWorld: ServerWorld
    val blockPos: BlockPosition

    fun teleport(world: ServerWorld, pos: Vector3d, yaw: Float, pitch: Float)
    fun setSpawnPoint(world: ServerWorld, spawn: BlockPosition, angle: Float, forced: Boolean, sendMessage: Boolean)
}

enum class PlayerSoundEvent {
    BLOCK_LEVER_CLICK,
    ENTITY_PLAYER_LEVELUP,
    ENTITY_SHULKER_AMBIENT,
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
