package me.jfenn.bingo.impl

import me.jfenn.bingo.api.*
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.utils.toNbt
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.passive.BatEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d
import java.util.*

object EntityManagerImpl : IEntityManager {
    private fun EntityType<*>.toMinecraftType(): net.minecraft.entity.EntityType<*> {
        return when (this) {
            EntityType.INTERACTION -> net.minecraft.entity.EntityType.INTERACTION
            EntityType.TEXT_DISPLAY -> net.minecraft.entity.EntityType.TEXT_DISPLAY
            EntityType.ARMOR_STAND -> net.minecraft.entity.EntityType.ARMOR_STAND
            EntityType.BAT -> net.minecraft.entity.EntityType.BAT
            else -> error("Entity type not recognized!")
        }
    }

    private fun forEntity(entity: Entity): IEntity {
        val impl = when (entity) {
            is InteractionEntity -> InteractionEntityImpl(entity)
            is TextDisplayEntity -> TextDisplayEntityImpl(entity)
            is ArmorStandEntity -> ArmorStandEntityImpl(entity)
            is BatEntity -> BatEntityImpl(entity)
            is LivingEntity -> LivingEntityImpl(entity)
            else -> EntityImpl(entity)
        }
        return impl
    }

    override fun <T : IEntity> createEntity(type: EntityType<T>, world: ServerWorld): T {
        val mcType = type.toMinecraftType()
        val entity = mcType.create(world, SpawnReason.STRUCTURE)!!
        val impl = forEntity(entity)
        return (@Suppress("UNCHECKED_CAST") (impl as T))
    }

    override fun getEntity(world: ServerWorld, uuid: UUID): IEntity? {
        val entity = world.getEntity(uuid) ?: return null
        return forEntity(entity)
    }

    override fun spawnEntity(world: ServerWorld, entity: IEntity): Boolean {
        return world.spawnEntity(entity.entity)
    }

    override fun iterateEntities(world: ServerWorld): Sequence<IEntity> {
        return world.iterateEntities()
            .asSequence()
            .map { forEntity(it) }
    }
}

open class EntityImpl(
    override val entity: Entity
) : IEntity {
    override var uuid: UUID
        get() = entity.uuid
        set(value) {
            entity.uuid = value
        }

    override var pos: Vector3d
        get() = entity.pos.let { Vector3d(it.x, it.y, it.z) }
        set(value) {
            entity.setPosition(value.x, value.y, value.z)
        }

    override var commandTags: Set<String>
        get() = entity.commandTags.toSet()
        set(value) {
            entity.commandTags.toSet().forEach { entity.removeCommandTag(it) }
            value.forEach { entity.addCommandTag(it) }
        }

    override var pitch: Float
        get() = entity.pitch
        set(value) {
            entity.pitch = value
        }

    override var yaw: Float
        get() = entity.yaw
        set(value) {
            entity.yaw = value
        }

    override fun discard() {
        entity.discard()
    }

    internal var nbt
        get() = NbtCompound().also { entity.writeNbt(it) }
        set(value) {
            entity.readNbt(value)
        }

    internal fun <R> patchNbt(patch: NbtCompound.() -> R): R {
        val compound = nbt
        val ret = patch(compound)
        nbt = compound
        return ret
    }
}

class InteractionEntityImpl(
    override val entity: InteractionEntity
) : IInteractionEntity, EntityImpl(entity) {
    override var width: Float
        get() = nbt.getFloat("width")
        set(value) {
            patchNbt { putFloat("width", value) }
        }
    override var height: Float
        get() = nbt.getFloat("height")
        set(value) {
            patchNbt {
                putFloat("height", value)
            }
        }
}

class TextDisplayEntityImpl(
    override val entity: TextDisplayEntity
) : ITextDisplayEntity, EntityImpl(entity) {
    private val koin = BingoKoin.getScope(entity.server)!!
    private val textSerializer by koin.inject<ITextSerializer>()

    override var value: Text
        get() = nbt.getString("text")
            ?.let { textSerializer.fromJson(it) }
            ?: Text.empty()
        set(value) {
            patchNbt {
                putString("text", textSerializer.toJson(value))
            }
        }

    override var lineWidth: Int
        get() = nbt.getInt("line_width")
        set(value) {
            patchNbt { putInt("line_width", value) }
        }

    override var billboard: ITextDisplayEntity.Billboard
        get() = nbt.getString("billboard")
            .uppercase()
            .let { ITextDisplayEntity.Billboard.valueOf(it) }
        set(value) {
            patchNbt { putString("billboard", value.name.lowercase()) }
        }

    override var alignment: ITextDisplayEntity.TextAlignment
        get() = nbt.getString("alignment")
            .uppercase()
            .let { ITextDisplayEntity.TextAlignment.valueOf(it) }
        set(value) {
            patchNbt { putString("alignment", value.name.lowercase()) }
        }

    override var background: Int
        get() = nbt.getInt("background")
        set(value) {
            patchNbt { putInt("background", value) }
        }

    override var shadow: Boolean
        get() = nbt.getBoolean("shadow")
        set(value) {
            patchNbt { putBoolean("shadow", value) }
        }

    override var transformation: Matrix4f
        get() = Matrix4f()
        set(value) {
            patchNbt { put("transformation", value.toNbt()) }
        }
}

class ArmorStandEntityImpl(
    override val entity: ArmorStandEntity,
) : IArmorStandEntity, EntityImpl(entity) {
    override fun equipStack(slot: IArmorStandEntity.EquipmentSlot, stack: IItemStack) {
        val mcSlot = when (slot) {
            IArmorStandEntity.EquipmentSlot.MAINHAND -> EquipmentSlot.MAINHAND
            IArmorStandEntity.EquipmentSlot.OFFHAND -> EquipmentSlot.OFFHAND
            IArmorStandEntity.EquipmentSlot.HEAD -> EquipmentSlot.HEAD
            IArmorStandEntity.EquipmentSlot.CHEST -> EquipmentSlot.CHEST
            IArmorStandEntity.EquipmentSlot.LEGS -> EquipmentSlot.LEGS
            IArmorStandEntity.EquipmentSlot.FEET -> EquipmentSlot.FEET
        }
        entity.equipStack(mcSlot, stack.stack)
    }
}

open class LivingEntityImpl(
    override val entity: LivingEntity
) : ILivingEntity, EntityImpl(entity) {
    override var invulnerable: Boolean
        get() = nbt.getBoolean("Invulnerable")
        set(value) {
            patchNbt { putBoolean("Invulnerable", value) }
        }
    override var noGravity: Boolean
        get() = nbt.getBoolean("NoGravity")
        set(value) {
            patchNbt { putBoolean("NoGravity", value) }
        }
    override var silent: Boolean
        get() = nbt.getBoolean("Silent")
        set(value) {
            patchNbt { putBoolean("Silent", value) }
        }
    override var noAI: Boolean
        get() = nbt.getBoolean("NoAI")
        set(value) {
            patchNbt { putBoolean("NoAI", value) }
        }
    override var persistenceRequired: Boolean
        get() = nbt.getBoolean("PersistenceRequired")
        set(value) {
            patchNbt { putBoolean("PersistenceRequired", value) }
        }

    override fun addEffect(
        type: EffectType,
        duration: Int,
        amplifier: Int,
        ambient: Boolean,
        visible: Boolean
    ): IStatusEffectHandle {
        val instance = StatusEffectInstance(
            when (type) {
                EffectType.NIGHT_VISION -> StatusEffects.NIGHT_VISION
                EffectType.SLOWNESS -> StatusEffects.SLOWNESS
                EffectType.JUMP_BOOST -> StatusEffects.JUMP_BOOST
                EffectType.INVISIBILITY -> StatusEffects.INVISIBILITY
                EffectType.OTHER -> throw IllegalArgumentException("[StatusEffectsImpl] OTHER is not a valid effect type!")
            },
            duration,
            amplifier,
            ambient,
            visible
        )
        entity.addStatusEffect(instance)
        return StatusEffectHandle(instance)
    }

    override fun getEffects(): List<IStatusEffectHandle> {
        return entity.statusEffects.map { StatusEffectHandle(it) }
    }

    override fun removeEffect(effect: IStatusEffectHandle) {
        require(effect is StatusEffectHandle)
        entity.removeStatusEffect(effect.instance.effectType)
    }
}

class BatEntityImpl(
    override val entity: BatEntity
) : IBatEntity, LivingEntityImpl(entity)
