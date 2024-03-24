 package dev.lythium.pulsar.routes

import dev.lythium.pulsar.TicketStatus
import dev.lythium.pulsar.db.TicketsDB
import dev.lythium.pulsar.db.UserDB
import dev.lythium.pulsar.responses.TicketCreateResponse
import dev.lythium.pulsar.responses.TicketMessageCreate
import dev.lythium.pulsar.responses.TicketStatusUpdateResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.ticketRoutes() {
	fun ApplicationCall.getUUIDFromParameter(param: String): UUID? {
		return try {
			UUID.fromString(parameters[param])
		} catch (e: IllegalArgumentException) {
			null
		}
	}

	suspend fun ApplicationCall.respondBadRequest(message: String) {
		this.respond(HttpStatusCode.BadRequest, TicketCreateResponse(error = message))
	}

	suspend fun ApplicationCall.respondNotFound(message: String) {
		this.respond(HttpStatusCode.NotFound, TicketCreateResponse(error = message))
	}

	fun getTicketStatus(status: String): TicketStatus? {
		return try {
			TicketStatus.valueOf(status.uppercase())
		} catch (e: IllegalArgumentException) {
			null
		}
	}

	route("/ticket") {
		route("/{id}") {
			get {
				val ticketId = call.getUUIDFromParameter("id") ?: return@get call.respondBadRequest("Invalid addon parameter.")
				val ticket = TicketsDB.get(id = ticketId) ?: return@get call.respondNotFound("Ticket not found.")

				call.respond(HttpStatusCode.OK, ticket)
			}

			patch("status") {
				val ticketId = call.getUUIDFromParameter("id") ?: return@patch call.respondBadRequest("Invalid addon parameter.")
				TicketsDB.get(id = ticketId) ?: return@patch call.respondNotFound("Ticket not found.")
				val status = call.parameters["status"] ?: return@patch call.respondBadRequest("Missing state parameter.")
				val ticketStatus = getTicketStatus(status) ?: return@patch call.respondBadRequest("Invalid status parameter.")

				try {
					val success = TicketsDB.updateState(ticketId, ticketStatus)
					call.respond(HttpStatusCode.OK, TicketStatusUpdateResponse(success = success, status = ticketStatus.name))
				} catch (e: Exception) {
					call.respondBadRequest(e.message.toString())
				}
			}

			route("/message") {
				post {
					val ticketId = call.getUUIDFromParameter("id") ?: return@post call.respondBadRequest("invalid id parameter.")
					TicketsDB.get(id = ticketId) ?: return@post call.respondNotFound("Ticket not found.")
					val userId = call.getUUIDFromParameter("user") ?: return@post call.respondBadRequest("Invalid user parameter.")
					UserDB.get(id = userId) ?: return@post call.respondNotFound("User not found.")
					val message = call.parameters["message"] ?: return@post call.respondBadRequest("Missing message parameter.")

					try {
						val msgId = TicketsDB.addMessage(ticketId, userId, message)
						call.respond(HttpStatusCode.Created, TicketMessageCreate(id = msgId))
					} catch (e: Exception) {
						call.respond(HttpStatusCode.BadRequest, TicketStatusUpdateResponse(error = e.message.toString()))
					}
				}

				patch {
					val ticketId = call.getUUIDFromParameter("id") ?: return@patch call.respondBadRequest("Invalid addon parameter.")
					TicketsDB.get(id = ticketId) ?: return@patch call.respondNotFound("Ticket not found.")
					val userId = call.getUUIDFromParameter("user") ?: return@patch call.respondBadRequest("Invalid user parameter.")
					UserDB.get(id = userId) ?: return@patch call.respondBadRequest("User not found.")
					val updatedFromId = call.getUUIDFromParameter("updated_from") ?: return@patch call.respondBadRequest("Invalid updated_from parameter.")
					TicketsDB.getMessage(updatedFromId) ?: return@patch call.respondBadRequest("Updated message not found.")
					val message = call.parameters["message"] ?: return@patch call.respondBadRequest("Missing message parameter.")

					try {
						val msgId = TicketsDB.addUpdatedMessage(ticketId, userId, message, updatedFromId)
						call.respond(HttpStatusCode.Created, TicketMessageCreate(id = msgId))
					} catch (e: Exception) {
						call.respondBadRequest(e.message.toString())
					}
				}
			}
		}

		post("/create") {
			val creator = call.parameters["creator"]?.toLongOrNull() ?: return@post call.respondBadRequest("Missing creator parameter.")
			val user = UserDB.get(discordId = creator) ?: return@post call.respondBadRequest("User not found.")
			val addonId = call.getUUIDFromParameter("addon") ?: return@post call.respondBadRequest("Invalid addon parameter.")
			user.id ?: return@post call.respondBadRequest("User not found.")

			try {
				val ticketId = TicketsDB.create(user.id, addonId)
				call.respond(HttpStatusCode.Created, TicketCreateResponse(id = ticketId))
			} catch (e: Exception) {
				call.respondBadRequest(e.message.toString())
			}
		}
	}
}