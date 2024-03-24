package dev.lythium.pulsar.db

import dev.lythium.pulsar.Addons
import dev.lythium.pulsar.Ticket
import dev.lythium.pulsar.TicketStatus
import dev.lythium.pulsar.Tickets
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserHasOpenTicketException : Exception("User already has an open ticket for this addon.")
class InvalidAddonTicketException : Exception("Invalid addon.")

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

	fun create(user: UUID, addon: UUID, status: TicketStatus = TicketStatus.OPEN): UUID {
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
}