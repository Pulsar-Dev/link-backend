package dev.lythium.pulsar.routes

import dev.lythium.pulsar.User
import dev.lythium.pulsar.db.UserDB
import dev.lythium.pulsar.db.UserExistsException
import dev.lythium.pulsar.responses.UserCreateResponse
import dev.lythium.pulsar.responses.UserGetResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

fun Route.userRoutes() {
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

	route("/user") {
		post {
			val steamId = call.parameters["steam_id"]?.toLongOrNull()
				?: return@post call.respondBadRequest("Missing `steam_id` or invalid parameter.")
			val discordId = call.parameters["discord_id"]?.toLongOrNull()
				?: return@post call.respondBadRequest("Missing `discord_id` or invalid parameter.")
			val gmodstoreId = call.getUUIDFromParameter("gmodstore_id")
				?: return@post call.respondBadRequest("Missing `gmodstore_id` or invalid parameter.")

			try {
				val id = UserDB.create(steamId, discordId, gmodstoreId)
				call.respond(HttpStatusCode.Created, UserCreateResponse(id))
			} catch (e: UserExistsException) {
				val existingUser = UserDB.get(steamId = steamId, discordId = discordId, gmodstoreId = gmodstoreId)

				call.respond(
					HttpStatusCode.InternalServerError,
					UserCreateResponse(existingUser?.id, "User already exists.")
				)
			} catch (e: Exception) {
				call.respond(HttpStatusCode.InternalServerError, UserCreateResponse(null, "Internal Server Error"))
			}
		}

		route("/{id}") {
			get {
				val id = call.parameters["id"] ?: return@get call.respondBadRequest("Missing parameters.")


				val pulsarId: UUID?
				try {
					pulsarId = UUID.fromString(id)
				} catch (e: IllegalArgumentException) {
					return@get call.respondBadRequest("Invalid parameters.")
				}

				val user: User = UserDB.get(id = pulsarId) ?: return@get call.respondNotFound("User not found.")

				call.respond(HttpStatusCode.OK, user)
			}

			get("/addons") {
				val id: UUID?
				try {
					id = UUID.fromString(call.parameters["id"])
				} catch (e: IllegalArgumentException) {
					call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Invalid parameters."))
					return@get
				}

				id ?: return@get call.respondBadRequest("Invalid parameters.")

				val user = UserDB.get(id = id) ?: return@get call.respondNotFound("User not found.")

				val addons = user.getOwnedAddons()
				val addonsJson = Json.encodeToString(addons)

				call.respond(HttpStatusCode.OK, addonsJson)
			}

			route("/{type}") {
				get {
					val id = call.parameters["id"] ?: return@get call.respondBadRequest("Missing parameters.")
					val type = call.parameters["type"] ?: return@get call.respondBadRequest("Missing parameters.")

					val user: User?

					when (type) {
						"steam" -> {
							val steamId = id.toLongOrNull() ?: return@get call.respondBadRequest("Invalid parameters.")

							user = UserDB.get(steamId = steamId)
						}

						"discord" -> {
							val discordId = id.toLongOrNull() ?: return@get call.respondBadRequest("Invalid parameters.")

							user = UserDB.get(discordId = discordId)
						}

						"gmodstore" -> {
							val gmodstoreId: UUID?
							try {
								gmodstoreId = UUID.fromString(id.toString())
							} catch (e: IllegalArgumentException) {
								call.respond(
									HttpStatusCode.BadRequest,
									UserGetResponse(error = "Invalid `id` parameter`.")
								)
								return@get
							}

							user = UserDB.get(gmodstoreId = gmodstoreId)
						}

						else -> {
							return@get call.respondBadRequest("Invalid parameters.")
						}
					}

					user ?: return@get call.respondNotFound("User not found.")

					call.respond(HttpStatusCode.OK, user)
				}
			}
		}

	}
}