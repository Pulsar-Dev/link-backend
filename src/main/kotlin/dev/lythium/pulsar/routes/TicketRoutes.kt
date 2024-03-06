package dev.lythium.pulsar.routes

import dev.lythium.pulsar.db.TicketsDB
import dev.lythium.pulsar.db.UserDB
import dev.lythium.pulsar.responses.TicketCreateResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.ticketRoutes() {
	route("/ticket") {
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

		get("{id}") {
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


	}
}