package dev.lythium.pulsar.db

import dev.lythium.pulsar.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserHasOpenTicketException : Exception("User already has an open ticket for this addon.")
class InvalidAddonTicketException : Exception("Invalid addon.")
class TicketNotFoundException : Exception("Ticket not found.")

object TicketsDB {
	fun get(id: UUID? = null, user: UUID? = null, addon: UUID? = null): Ticket? {
		if (id == null && user == null) return null
		if (user != null && addon == null) return null

		val ticketData = transaction {
			Tickets.select {
				if (id != null) Tickets.id eq id
				else if (user != null && addon != null) (Tickets.user eq user) and (Tickets.addon eq addon)
				else return@transaction 0
			}.map {
				Ticket(
					id = it[Tickets.id].value,
					user = it[Tickets.user],
					addon = it[Tickets.addon],
					status = it[Tickets.status],
					created = it[Tickets.created].toKotlinLocalDateTime(),
					updated = it[Tickets.updated].toKotlinLocalDateTime()
				)
			}.firstOrNull()
		}

		if (ticketData !is Ticket) return null

		return ticketData
	}

	private fun userHasOpenTicket(user: UUID, addon: UUID? = null): Boolean {
		val ticket = get(user = user, addon = addon)
		return ticket != null && ticket.status == TicketStatus.OPEN
	}

	suspend fun create(user: UUID, addon: UUID, status: TicketStatus = TicketStatus.OPEN): UUID {
		if (userHasOpenTicket(user, addon)) throw UserHasOpenTicketException()

		Addons.getAddon(addon) ?: throw InvalidAddonTicketException()

		val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

		val insertedId = transaction {
			Tickets.insertAndGetId {
				it[Tickets.user] = user
				it[Tickets.addon] = addon
				it[Tickets.status] = status
				it[created] = currentTime.toJavaLocalDateTime()
				it[updated] = currentTime.toJavaLocalDateTime()
			}.value
		}

		return insertedId
	}

	fun updateState(id: UUID, state: TicketStatus): Boolean {
		get(id = id) ?: throw TicketNotFoundException()

		val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

		val updated = transaction {
			Tickets.update({ Tickets.id eq id }) {
				it[status] = state
				it[updated] = currentTime.toJavaLocalDateTime()
			}
		}

		return updated > 0
	}

	fun addMessage(id: UUID, user: UUID, message: String): UUID {
		get(id = id) ?: throw TicketNotFoundException()

		val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

		val insertedId = transaction {
			TicketMessages.insertAndGetId {
				it[ticket] = id
				it[TicketMessages.user] = user
				it[TicketMessages.message] = message
				it[created] = currentTime.toJavaLocalDateTime()
				it[updated_from] = null
			}.value
		}

		return insertedId
	}

	fun addUpdatedMessage(id: UUID, user: UUID, message: String, updatedFrom: UUID): UUID {
		get(id = id) ?: throw TicketNotFoundException()

		val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

		val insertedId = transaction {
			TicketMessages.insertAndGetId {
		 		it[ticket] = id
				it[TicketMessages.user] = user
				it[TicketMessages.message] = message
				it[created] = currentTime.toJavaLocalDateTime()
				it[updated_from] = updatedFrom
			}.value
		}

		return insertedId
	}
	
	fun getMessage(id: UUID): TicketMessage? {
		val messageData = transaction {
			TicketMessages.select { TicketMessages.id eq id }.map {
				TicketMessage(
					id = it[TicketMessages.id].value,
					ticket = it[TicketMessages.ticket].value,
					user = it[TicketMessages.user].value,
					message = it[TicketMessages.message],
					created = it[TicketMessages.created].toKotlinLocalDateTime(),
					updatedFrom = it[TicketMessages.updated_from]?.value
				)
			}.firstOrNull()
		}

		if (messageData !is TicketMessage) return null

		return messageData
	}
}