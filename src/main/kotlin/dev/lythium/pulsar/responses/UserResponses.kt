package dev.lythium.pulsar.responses

import dev.lythium.pulsar.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserCreateResponse(
	@Serializable(with = UUIDSerializer::class) val id: UUID
)