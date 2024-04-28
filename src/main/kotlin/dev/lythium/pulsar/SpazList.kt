package dev.lythium.pulsar

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

@Serializable
class SpazListUser(
	@Serializable(with = UUIDSerializer::class) val gmodstore_id: UUID? = null,
)

object SpazList : UUIDTable() {
	val gmodstore_id = uuid("gmodstore_id")
}