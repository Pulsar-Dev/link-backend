package dev.lythium.pulsar

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.*

enum class TicketStatus {
	OPEN,
	HOLD,
	CLOSED
}


@Serializable
class Ticket(
	@Serializable(with = UUIDSerializer::class) val id: UUID? = null,
	@Serializable(with = UUIDSerializer::class) val user: UUID? = null,
	@Serializable(with = UUIDSerializer::class) val addon: UUID? = null,
	val status: TicketStatus,
	@Transient val created: LocalDateTime? = null,
	@Transient val updated: LocalDateTime? = null
)

@Serializable
class TicketMessage(
	@Serializable(with = UUIDSerializer::class) val id: UUID? = null,
	@Serializable(with = UUIDSerializer::class) val ticket: UUID? = null,
	@Serializable(with = UUIDSerializer::class) val user: UUID? = null,
	val message: String,
	@Transient val created: LocalDateTime? = null,
	@Transient val updatedFrom: UUID? = null
)

object Tickets : UUIDTable() {
	val user = uuid("user")
	val addon = uuid("addon")
	val status = enumeration("status", TicketStatus::class)
	val created = datetime("created")
	val updated = datetime("updated")
}

object TicketMessages: UUIDTable() {
	val ticket = reference("ticket", Tickets)
	val user = reference("user", Users)
	val message = text("message")
	val created = datetime("created")
	val updated_from = reference("updated_from", TicketMessages).nullable()
}