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
	route("/user") {
		post {
			val steamId = call.parameters["steam_id"]?.toLongOrNull()
			val discordId = call.parameters["discord_id"]?.toLongOrNull()
			val gmodstoreId: String? = call.parameters["gmodstore_id"]

			val gmsId: UUID?
			try {
				gmsId = UUID.fromString(gmodstoreId.toString())
			} catch (e: IllegalArgumentException) {
				call.respond(HttpStatusCode.BadRequest, UserCreateResponse(null, "Invalid parameters."))
				return@post
			}

			if (steamId == null || discordId == null || gmsId == null) {
				call.respond(HttpStatusCode.BadRequest, UserCreateResponse(null, "Missing parameters."))
				return@post
			}


			try {
				val id = UserDB.create(steamId, discordId, gmsId)
				call.respond(HttpStatusCode.Created, UserCreateResponse(id))
			} catch (e: UserExistsException) {
				val existingUser = UserDB.get(steamId = steamId, discordId = discordId, gmodstoreId = gmsId)

				call.respond(HttpStatusCode.InternalServerError, UserCreateResponse(existingUser?.id, "User already exists."))
			} catch (e: Exception) {
				call.respond(HttpStatusCode.InternalServerError, UserCreateResponse(null, "Internal Server Error"))
			}
		}

		route("/{id}") {
			get {
				val id = call.parameters["id"]

				if (id == null) {
					call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Missing parameters."))
					return@get
				}

				val pulsarId: UUID?
				try {
					pulsarId = UUID.fromString(id.toString())
				} catch (e: IllegalArgumentException) {
					call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Invalid parameters."))
					return@get
				}

				val user: User? = UserDB.get(id = pulsarId)

				if (user == null) {
					call.respond(HttpStatusCode.NotFound, UserGetResponse(error = "User not found."))
					return@get
				}

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

				if (id == null) {
					call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Missing parameters."))
					return@get
				}

				val user = UserDB.get(id = id)

				if (user == null) {
					call.respond(HttpStatusCode.NotFound, UserGetResponse(error = "User not found."))
					return@get
				}

				val addons = user.getOwnedAddons()
				val addonsJson = Json.encodeToString(addons)

				call.respond(HttpStatusCode.OK, addonsJson)
			}

			route("/{type}") {
				get {
					val id = call.parameters["id"]
					val type = call.parameters["type"]

					if (id == null) {
						call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Missing parameters."))
						return@get
					}

					val user: User?

					when (type) {
						"steam" -> {
							val steamId: Long = (id.toLongOrNull() ?: run {
								call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Invalid parameters."))
								return@get
							})

							user = UserDB.get(steamId = steamId)
						}

						"discord" -> {
							val discordId: Long = (id.toLongOrNull() ?: run {
								call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Invalid parameters."))
								return@get
							})

							user = UserDB.get(discordId = discordId)
						}

						"gmodstore" -> {
							val gmodstoreId: UUID?
							try {
								gmodstoreId = UUID.fromString(id.toString())
							} catch (e: IllegalArgumentException) {
								call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Invalid `id` parameter`."))
								return@get
							}

							user = UserDB.get(gmodstoreId = gmodstoreId)
						}

						else -> {
							call.respond(HttpStatusCode.BadRequest, UserGetResponse(error = "Invalid parameters."))
							return@get
						}
					}

					if (user == null) {
						call.respond(HttpStatusCode.NotFound, UserGetResponse(error = "User not found."))
						return@get
					}

					call.respond(HttpStatusCode.OK, user)
				}
			}
		}

	}
}