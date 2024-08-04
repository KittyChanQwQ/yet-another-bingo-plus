package me.jfenn.bingo.common.card.data

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import me.jfenn.bingo.common.card.CardGeneratorState

@Serializable
sealed class NumberProvider {

    @Serializable
    @SerialName("random")
    class Random(
        val min: Int = 1,
        val max: Int = 64,
    ) : NumberProvider() {
        override fun resolve(state: CardGeneratorState): Int {
            return state.random.nextInt(min, max + 1)
        }
    }

    @Serializable
    @SerialName("constant")
    class Constant(val value: Int) : NumberProvider() {
        override fun resolve(state: CardGeneratorState) = value
    }

    abstract fun resolve(state: CardGeneratorState): Int

}

typealias NumberProviderPolymorphic = @Serializable(with = NumberProviderSerializer::class) NumberProvider

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
object NumberProviderSerializer : KSerializer<NumberProviderPolymorphic> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("NumberProvider", PolymorphicKind.SEALED)

    private val baseSerializer get() = NumberProvider.serializer()

    override fun deserialize(decoder: Decoder): NumberProviderPolymorphic {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return when {
            element is JsonPrimitive && element.intOrNull != null -> NumberProvider.Constant(element.int)
            else -> decoder.json.decodeFromJsonElement(baseSerializer, element)
        }
    }

    override fun serialize(encoder: Encoder, value: NumberProviderPolymorphic) {
        when (value) {
            is NumberProvider.Constant -> encoder.encodeInt(value.value)
            else -> baseSerializer.serialize(encoder, value)
        }
    }
}