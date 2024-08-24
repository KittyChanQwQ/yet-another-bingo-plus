package me.jfenn.bingo.api

import me.jfenn.bingo.api.item.IItemStack
import net.minecraft.entity.Entity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d
import java.util.*

interface IEntityManager {
    fun <T: IEntity> createEntity(type: EntityType<T>, world: ServerWorld): T
    fun getEntity(world: ServerWorld, uuid: UUID): IEntity?
    fun spawnEntity(world: ServerWorld, entity: IEntity): Boolean
    fun iterateEntities(world: ServerWorld): Sequence<IEntity>
}

class EntityType<T: IEntity> {
    companion object {
        val TEXT_DISPLAY = EntityType<ITextDisplayEntity>()
        val INTERACTION = EntityType<IInteractionEntity>()
        val ARMOR_STAND = EntityType<IArmorStandEntity>()
        val BAT = EntityType<IBatEntity>()
        val OTHER = EntityType<IBatEntity>()
    }
}

interface IEntity {
    val type: EntityType<*> get() = EntityType.OTHER
    val entity: Entity
    var uuid: UUID
    var pos: Vector3d
    var commandTags: Set<String>
    var pitch: Float
    var yaw: Float

    fun discard()
}

interface ILivingEntity : IEntity {
    var invulnerable: Boolean
    var noGravity: Boolean
    var silent: Boolean
    var noAI: Boolean
    var persistenceRequired: Boolean

    fun addEffect(type: EffectType, duration: Int, amplifier: Int, ambient: Boolean, visible: Boolean): IStatusEffectHandle
    fun getEffects(): List<IStatusEffectHandle>
    fun removeEffect(effect: IStatusEffectHandle)
}

interface IInteractionEntity : IEntity {
    override val type get() = EntityType.INTERACTION
    var width: Float
    var height: Float
}

interface IDisplayEntity : IEntity {
    var transformation: Matrix4f
}

interface ITextDisplayEntity : IDisplayEntity {
    enum class Billboard { FIXED, VERTICAL }
    enum class TextAlignment { CENTER, LEFT, RIGHT }

    override val type get() = EntityType.TEXT_DISPLAY
    var value: Text
    var lineWidth: Int
    var billboard: Billboard
    var alignment: TextAlignment
    var background: Int
    var shadow: Boolean
}

interface IArmorStandEntity : IEntity {
    override val type get() = EntityType.ARMOR_STAND

    enum class EquipmentSlot { MAINHAND, OFFHAND, FEET, LEGS, CHEST, HEAD }
    fun equipStack(slot: EquipmentSlot, stack: IItemStack)
}

interface IBatEntity : ILivingEntity {
    override val type get() = EntityType.BAT
}
