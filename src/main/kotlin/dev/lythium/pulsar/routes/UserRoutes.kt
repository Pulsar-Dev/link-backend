package dev.lythium.pulsar.routes

import dev.lythium.pulsar.Environment
import dev.lythium.pulsar.User
import dev.lythium.pulsar.db.UserDB
import dev.lythium.pulsar.db.UserExistsException
import dev.lythium.pulsar.responses.UserCreateResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

fun Route.userRoutes() {
	route("/user") {
		post {
			if (call.request.header("Authorization") != Environment.dotenv.get("API_KEY")) {
				call.respond(HttpStatusCode.Unauthorized, "Unauthorized.")
				return@post
			}

			val steamId = call.parameters["steamId"]?.toLongOrNull()
			val discordId = call.parameters["discordId"]?.toLongOrNull()
			val gmodstoreId = UUID.fromString(call.parameters["gmodstoreId"])

			if (steamId == null || discordId == null || gmodstoreId == null) {
				call.respond(HttpStatusCode.BadRequest, "Missing parameters.")
				return@post
			}

			try {
				val id = UserDB.create(steamId, discordId, gmodstoreId)
				call.respond(HttpStatusCode.Created, UserCreateResponse(id))
			} catch (e: UserExistsException) {
				call.respond(HttpStatusCode.Conflict, e.message ?: "User already exists.")
			} catch (e: Exception) {
				call.respond(HttpStatusCode.InternalServerError, "Internal Server Error.")
			}
		}

		route("/{id}") {
			get {
				if (call.request.header("Authorization") != Environment.dotenv.get("API_KEY")) {
					call.respond(HttpStatusCode.Unauthorized, "Unauthorized.")
					return@get
				}

				val id = call.parameters["id"]

				if (id == null) {
					call.respond(HttpStatusCode.BadRequest, "Missing parameters.")
					return@get
				}

				val pulsarId: UUID?
				try {
					pulsarId = UUID.fromString(id.toString())
				} catch (e: IllegalArgumentException) {
					call.respond(HttpStatusCode.BadRequest, "Invalid parameters.")
					return@get
				}

				val user: User? = UserDB.get(id = pulsarId)

				if (user == null) {
					call.respond(HttpStatusCode.NotFound, "User not found.")
					return@get
				}

				call.respond(HttpStatusCode.OK, user)
			}

			get("/addons") {
				if (call.request.header("Authorization") != Environment.dotenv.get("API_KEY")) {
					call.respond(HttpStatusCode.Unauthorized, "Unauthorized.")
					return@get
				}

				val id: UUID?
				try {
					id = UUID.fromString(call.parameters["id"])
				} catch (e: IllegalArgumentException) {
					call.respond(HttpStatusCode.BadRequest, "Invalid parameters.")
					return@get
				}

				if (id == null) {
					call.respond(HttpStatusCode.BadRequest, "Missing parameters.")
					return@get
				}

				val user = UserDB.get(id = id)

				if (user == null) {
					call.respond(HttpStatusCode.NotFound, "User not found.")
					return@get
				}

				val addons = user.getOwnedAddons()
				val addonsJson = Json.encodeToString(addons)

				call.respond(HttpStatusCode.OK, addonsJson)

			}

			route("/{type}") {
				get {
					if (call.request.header("Authorization") != Environment.dotenv.get("API_KEY")) {
						call.respond(HttpStatusCode.Unauthorized, "Unauthorized.")
						return@get
					}

					val id = call.parameters["id"]
					val type = call.parameters["type"]

					if (id == null) {
						call.respond(HttpStatusCode.BadRequest, "Missing parameters.")
						return@get
					}

					val user: User?

					when (type) {
						"steam" -> {
							val steamId: Long = (id.toLongOrNull() ?: run {
								call.respond(HttpStatusCode.BadRequest, "Invalid parameters.")
								return@get
							})

							user = UserDB.get(steamId = steamId)
						}

						"discord" -> {
							val discordId: Long = (id.toLongOrNull() ?: run {
								call.respond(HttpStatusCode.BadRequest, "Invalid parameters.")
								return@get
							})

							user = UserDB.get(discordId = discordId)
						}

						"gmodstore" -> {
							val gmodstoreId: UUID?
							try {
								gmodstoreId = UUID.fromString(id.toString())
							} catch (e: IllegalArgumentException) {
								call.respond(HttpStatusCode.BadRequest, "Invalid id parameter.")
								return@get
							}

							user = UserDB.get(gmodstoreId = gmodstoreId)
						}

						else -> {
							call.respond(HttpStatusCode.BadRequest, "Invalid parameters.")
							return@get
						}
					}

					if (user == null) {
						call.respond(HttpStatusCode.NotFound, "User not found.")
						return@get
					}

					call.respond(HttpStatusCode.OK, user)
				}
			}
		}

	}
}