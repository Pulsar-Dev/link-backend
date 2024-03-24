package dev.lythium.pulsar.responses

import dev.lythium.pulsar.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TicketCreateResponse(
	@Serializable(with = UUIDSerializer::class) val id: UUID? = null,
	val error: String? = null
)

@Serializable
data class TicketStatusUpdateResponse(
	val status: String? = null,
	val success: Boolean? = null,
	val error: String? = null
)

@Serializable
data class TicketMessageCreate(
	@Serializable(with = UUIDSerializer::class) val id: UUID? = null,
	val error: String? = null
)