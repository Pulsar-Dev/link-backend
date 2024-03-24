package dev.lythium.pulsar.responses

import dev.lythium.pulsar.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TicketCreateResponse(
	@Serializable(with = UUIDSerializer::class) val id: UUID? = null,
	val error: String? = null
)