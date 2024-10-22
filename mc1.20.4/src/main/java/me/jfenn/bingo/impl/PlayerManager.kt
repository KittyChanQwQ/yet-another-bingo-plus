package me.jfenn.bingo.impl

import com.mojang.authlib.GameProfile
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.PlayerSoundCategory
import me.jfenn.bingo.api.PlayerSoundEvent
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.team.BingoPlayerProfile
import net.minecraft.command.CommandSource
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.BoatItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.common.SyncedClientOptions
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.UseAction
import net.minecraft.util.hit.HitResult
import org.joml.Vector3d
import java.util.*

class PlayerManager(
    private val server: MinecraftServer,
) : IPlayerManager {
    override fun forPlayer(player: ServerPlayerEntity): IPlayerHandle {
        return PlayerHandle(player)
    }

    override fun getPlayer(uuid: UUID): IPlayerHandle? {
        return server.playerManager.getPlayer(uuid)
            ?.let { PlayerHandle(it) }
    }

    override fun getPlayers(): List<IPlayerHandle> {
        return server.playerManager.playerList
            .map { PlayerHandle(it) }
    }

    override fun getOfflinePlayer(profile: BingoPlayerProfile): IPlayerHandle {
        val player = ServerPlayerEntity(
            server,
            server.overworld,
            GameProfile(profile.uuid, profile.name),
            SyncedClientOptions.createDefault(),
        )

        server.playerManager.loadPlayerData(player)
        return PlayerHandle(player)
    }

    override fun playToAround(
        player: ServerPlayerEntity?,
        sound: PlayerSoundEvent,
        category: PlayerSoundCategory,
        volume: Float,
        pitch: Float,
        position: Triple<Double, Double, Double>,
        world: ServerWorld,
    ) {
        val (x, y, z) = position
        val soundEntry = Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_LEVER_CLICK)
        val packet = PlaySoundS2CPacket(soundEntry, category.toSoundCategory(), x, y, z, volume, pitch, server.overworld.seed)
        player?.networkHandler?.sendPacket(packet)
        server.playerManager.sendToAround(player, x, y, z, soundEntry.value().getDistanceToTravel(volume).toDouble(), world.registryKey, packet)
    }
}

class PlayerHandle(
    override val player: ServerPlayerEntity
) : IPlayerHandle, LivingEntityImpl(player) {

    private val itemStackFactory: IItemStackFactory by lazy {
        BingoKoin.getScope(player.server)!!.get<IItemStackFactory>()
    }

    override val world: ServerWorld
        get() = player.serverWorld

    override val isAlive: Boolean
        get() = player.isAlive
    override val isSpectator: Boolean
        get() = player.isSpectator

    override val name: String
        get() = player.nameForScoreboard

    override val commandSource: CommandSource
        get() = player.commandSource

    override var fireTicks: Int by player::fireTicks
    override var isOnFire: Boolean
        get() = player.isOnFire
        set(value) {
            player.isOnFire = value
        }

    override var foodLevel: Int by player.hungerManager::foodLevel
    override var saturationLevel: Float by player.hungerManager::saturationLevel
    override var exhaustion: Float by player.hungerManager::exhaustion

    override fun canUseItem(stack: IItemStack): Boolean {
        val itemStack: ItemStack = stack.stack
        return when {
            itemStack.isEmpty -> false
            itemStack.useAction == UseAction.EAT -> player.canConsume(false)
            itemStack.useAction == UseAction.DRINK -> true
            itemStack.useAction == UseAction.BLOCK -> true // blocking with a shield
            itemStack.item is BoatItem -> true // trying to place a boat
            itemStack.item is BlockItem -> {
                // if holding a block, check if it can be placed on a surface within reach
                val hit = player.raycast(5.0, 0f, false)
                hit.type == HitResult.Type.BLOCK
            }
            itemStack.item == Items.FIREWORK_ROCKET -> player.isFallFlying || player.isTouchingWater
            itemStack.item == Items.SUSPICIOUS_STEW -> true
            itemStack.isUsedOnRelease -> true
            itemStack.useAction == UseAction.NONE -> false
            else -> true
        }
    }

    override fun allHeldStacks(): Sequence<IItemStack> {
        return sequence {
            // yield all items in inventory
            yieldAll(allInventorySlots().map { (_, stack) -> stack })

            // yield the current cursor stack
            yield(itemStackFactory.forStack(player.currentScreenHandler.cursorStack))
        }
    }

    override fun allInventorySlots(): Sequence<Pair<Int, IItemStack>> {
        return sequence {
            // yield all items in inventory
            for (i in 0 until player.inventory.size()) {
                yield(Pair(i, player.inventory.getStack(i)))
            }
        }.map { (i, stack) ->
            i to itemStackFactory.forStack(stack)
        }
    }

    override fun giveOrEquipStack(stack: IItemStack) {
        val itemStack = stack.stack

        // if the item can be equipped (e.g. iron chestplate), put it in an equipment slot
        val slot = MobEntity.getPreferredEquipmentSlot(itemStack)
        if (slot != null && player.canEquip(itemStack) && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            player.equipStack(slot, itemStack)
        } else {
            // otherwise, put it in the player's inventory
            player.giveItemStack(itemStack)
        }
    }

    override fun giveItemStack(stack: IItemStack) {
        player.giveItemStack(stack.stack)
    }

    override fun removeStack(slot: Int) {
        player.inventory.removeStack(slot)
    }

    override fun setStack(slot: Int, stack: IItemStack) {
        player.getStackReference(slot).set(stack.stack)
    }

    override fun playSound(sound: PlayerSoundEvent, category: PlayerSoundCategory, volume: Float, pitch: Float) {
        player.playSound(
            sound.toSoundEvent(),
            category.toSoundCategory(),
            volume,
            pitch,
        )
    }

    override val serverWorld: ServerWorld by player::serverWorld

    override val blockPos: BlockPosition
        get() = BlockPosition(player.blockX, player.blockY, player.blockZ)

    override fun teleport(world: ServerWorld, pos: Vector3d, yaw: Float, pitch: Float) {
        player.teleport(world, pos.x, pos.y, pos.z, yaw, pitch)
    }

    override fun setSpawnPoint(
        world: ServerWorld,
        spawn: BlockPosition,
        angle: Float,
        forced: Boolean,
        sendMessage: Boolean
    ) {
        player.setSpawnPoint(world.registryKey, spawn.toBlockPos(), angle, forced, sendMessage)
    }
}

private fun PlayerSoundEvent.toSoundEvent() = when (this) {
    PlayerSoundEvent.BLOCK_LEVER_CLICK -> SoundEvents.BLOCK_LEVER_CLICK
    PlayerSoundEvent.ENTITY_PLAYER_LEVELUP -> SoundEvents.ENTITY_PLAYER_LEVELUP
    PlayerSoundEvent.ENTITY_SHULKER_AMBIENT -> SoundEvents.ENTITY_SHULKER_AMBIENT
    PlayerSoundEvent.BLOCK_NOTE_BLOCK_BASS -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value()
    PlayerSoundEvent.ITEM_LODESTONE_COMPASS_LOCK -> SoundEvents.ITEM_LODESTONE_COMPASS_LOCK
    PlayerSoundEvent.BLOCK_PORTAL_TRAVEL -> SoundEvents.BLOCK_PORTAL_TRAVEL
    PlayerSoundEvent.BLOCK_NOTE_BLOCK_PLING -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()
}

private fun PlayerSoundCategory.toSoundCategory() = when (this) {
    PlayerSoundCategory.MAIN -> SoundCategory.MASTER
    PlayerSoundCategory.RECORDS -> SoundCategory.RECORDS
    PlayerSoundCategory.BLOCKS -> SoundCategory.BLOCKS
}
