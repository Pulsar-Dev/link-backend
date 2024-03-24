 package dev.lythium.pulsar.routes

import dev.lythium.pulsar.TicketStatus
import dev.lythium.pulsar.db.TicketsDB
import dev.lythium.pulsar.db.UserDB
import dev.lythium.pulsar.responses.TicketCreateResponse
import dev.lythium.pulsar.responses.TicketStatusUpdateResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.ticketRoutes() {
	route("/ticket") {
		route("/{id}") {
			get {
				val id = call.parameters["id"]

				val ticketId: UUID?
				try {
					ticketId = UUID.fromString(id.toString())
				} catch (e: IllegalArgumentException) {
					call.respond(HttpStatusCode.BadRequest, TicketCreateResponse(error = "Invalid addon parameter."))
					return@get
				}

				val ticket = TicketsDB.get(id = ticketId)
				if (ticket == null) {
					call.respond(HttpStatusCode.NotFound, TicketCreateResponse(error = "Ticket not found."))
					return@get
				}

				call.respond(HttpStatusCode.OK, ticket)
			}

			patch("status") {
				val id = call.parameters["id"]
				val status = call.parameters["status"]

				val ticketId: UUID?
				try {
					ticketId = UUID.fromString(id.toString())
				} catch (e: IllegalArgumentException) {
					call.respond(HttpStatusCode.BadRequest, TicketStatusUpdateResponse(error = "Invalid addon parameter."))
					return@patch
				}

				val ticket = TicketsDB.get(id = ticketId)
				if (ticket == null) {
					call.respond(HttpStatusCode.NotFound, TicketStatusUpdateResponse(error = "Ticket not found."))
					return@patch
				}

				if (status == null) {
					call.respond(HttpStatusCode.BadRequest, TicketStatusUpdateResponse(error = "Missing state parameter."))
					return@patch
				}

				val ticketStatus: TicketStatus
				try {
					ticketStatus = TicketStatus.valueOf(status.uppercase())
				} catch (e: IllegalArgumentException) {
					call.respond(HttpStatusCode.BadRequest, TicketStatusUpdateResponse(error = "Invalid state parameter."))
					return@patch
				}

				try {
					val success = TicketsDB.updateState(ticketId, ticketStatus)

					call.respond(HttpStatusCode.OK, TicketStatusUpdateResponse(success = success, status = ticketStatus.name))
				} catch (e: Exception) {
					call.respond(HttpStatusCode.BadRequest, TicketStatusUpdateResponse(error = e.message.toString()))
				}
			}
		}

		post("/create") {
			val creator = call.parameters["creator"]?.toLongOrNull()
			val addon = call.parameters["addon"]

			if (creator == null) {
				call.respond(HttpStatusCode.BadRequest, TicketCreateResponse(error = "Missing creator parameter."))
				return@post
			}

			val user = UserDB.get(discordId = creator)
			if (user?.id == null) {
				call.respond(HttpStatusCode.BadRequest, TicketCreateResponse(error = "User not found."))
				return@post
			}


			val addonId: UUID?
			try {
				addonId = UUID.fromString(addon.toString())
			} catch (e: IllegalArgumentException) {
				call.respond(HttpStatusCode.BadRequest, TicketCreateResponse(error = "Invalid addon parameter."))
				return@post
			}

			val ticketId: UUID

			try {
				ticketId = TicketsDB.create(user.id, addonId)
			} catch (e: Exception) {
				call.respond(HttpStatusCode.BadRequest, TicketCreateResponse(error = e.message.toString()))
				return@post
			}

			call.respond(HttpStatusCode.Created, TicketCreateResponse(id = ticketId))
		}
	}
}