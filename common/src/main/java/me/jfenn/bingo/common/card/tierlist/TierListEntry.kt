package me.jfenn.bingo.common.card.tierlist

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = TierListEntry.Serializer::class)
sealed class TierListEntry {

    @Deprecated("No longer used. This has no effect on the generated cards.")
    data object MultiEntry : TierListEntry() {
        override fun asList(): List<String> = emptyList()
    }

    data class SingleEntry(val item: String) : TierListEntry() {
        override fun asList() = listOf(item)
    }

    abstract fun asList(): List<String>

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    companion object Serializer : KSerializer<TierListEntry> {

        override val descriptor: SerialDescriptor = buildSerialDescriptor("TierListEntry", PolymorphicKind.SEALED)

        private val stringSerializer get() = String.serializer()
        private val listSerializer get() = ListSerializer(String.serializer())

        override fun deserialize(decoder: Decoder): TierListEntry {
            val element = (decoder as JsonDecoder).decodeJsonElement()
            return when {
                element is JsonArray -> MultiEntry
                else -> SingleEntry(element.jsonPrimitive.content)
            }
        }

        override fun serialize(encoder: Encoder, value: TierListEntry) {
            when (value) {
                is MultiEntry -> listSerializer.serialize(encoder, emptyList())
                is SingleEntry -> stringSerializer.serialize(encoder, value.item)
            }
        }
    }

}