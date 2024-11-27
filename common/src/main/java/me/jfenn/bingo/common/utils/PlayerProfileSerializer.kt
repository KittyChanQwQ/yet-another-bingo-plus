package me.jfenn.bingo.common.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import me.jfenn.bingo.platform.player.PlayerProfile

typealias PlayerProfileType = @Serializable(with = PlayerProfileSerializer::class) PlayerProfile

object PlayerProfileSerializer : KSerializer<PlayerProfile> {
    override val descriptor = buildClassSerialDescriptor(PlayerProfile::class.qualifiedName!!) {
        element<String>("name")
        element("uuid", UuidSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): PlayerProfile {
        return decoder.decodeStructure(descriptor) {
            val name = decodeStringElement(descriptor, 0)
            val uuid = decodeSerializableElement(descriptor, 1, UuidSerializer)
            PlayerProfile(uuid = uuid, name = name)
        }
    }

    override fun serialize(encoder: Encoder, value: PlayerProfile) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
            encodeSerializableElement(descriptor, 1, UuidSerializer, value.uuid)
        }
    }
}