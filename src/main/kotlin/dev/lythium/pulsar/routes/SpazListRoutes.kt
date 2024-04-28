package dev.lythium.pulsar.routes

import dev.lythium.pulsar.db.SpazListDB
import dev.lythium.pulsar.responses.UserCreateResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.spazListRoutes() {
	fun ApplicationCall.getUUIDFromParameter(param: String): UUID? {
		return try {
			UUID.fromString(parameters[param])
		} catch (e: IllegalArgumentException) {
			null
		}
	}

	suspend fun ApplicationCall.respondBadRequest(message: String) {
		this.respond(HttpStatusCode.BadRequest, UserCreateResponse(error = message))
	}

	suspend fun ApplicationCall.respondNotFound(message: String) {
		this.respond(HttpStatusCode.NotFound, UserCreateResponse(error = message))
	}

	route("/spazlist") {
		get {
			val users = SpazListDB.get();
			call.respond(HttpStatusCode.OK, users)
		}

		get("/{id}") {
			val id = call.getUUIDFromParameter("id")
				?: return@get call.respondBadRequest("Missing `id` or invalid parameter.")

			val user = SpazListDB.getByID(id)
			if (user == null) {
				call.respond(HttpStatusCode.OK, "")
				return@get
			}

			call.respond(HttpStatusCode.InternalServerError, "Internal Server Error.")
		}

		post("/{id}") {
			val id = call.getUUIDFromParameter("id")
				?: return@post call.respondBadRequest("Missing `id` or invalid parameter.")

			val status = SpazListDB.create(id)

			if (!status) {
				call.respond(HttpStatusCode.Conflict, "User already exists.")
				return@post
			}

			call.respond(HttpStatusCode.Created, "yup")
		}
	}
}