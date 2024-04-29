package dev.lythium.pulsar.routes

import dev.lythium.pulsar.Addons
import dev.lythium.pulsar.handlers.ReviewHandler
import dev.lythium.pulsar.handlers.TrackingHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.reviewRoutes() {
	fun ApplicationCall.getUUIDFromParameter(param: String): UUID? {
		return try {
			UUID.fromString(parameters[param])
		} catch (e: IllegalArgumentException) {
			null
		}
	}

	suspend fun ApplicationCall.nuhUh() {
		this.respond(HttpStatusCode(402, "bruh?"), "nuh uh")
	}

	route("/reviewers") {
		route("/{addon}") {
			get {
				val addon =
					call.getUUIDFromParameter("addon") ?: return@get call.respondText("Invalid addon parameter.")

				if (Addons.getAddon(addon) == null) {
					return@get call.respondText("Invalid addon")
				}

				val reviews = ReviewHandler.getAllReviewers(addon)

				call.respond(reviews)
			}
			get("/{userId}") {
				val addon =
					call.getUUIDFromParameter("addon") ?: return@get call.respondText("Invalid addon parameter.")
				val userId =
					call.getUUIDFromParameter("userId") ?: return@get call.respondText("Invalid userId parameter.")

				if (Addons.getAddon(addon) == null) {
					return@get call.respondText("Invalid addon")
				}

				val headers = call.request.headers

				val licensee = headers["Licensee"]
				val address = headers["Address"]
				val hostname = headers["Hostname"]
				val version = headers["Version"]
				val challenge = headers["Challenge"]

				if (licensee == null || address == null || hostname == null || version == null || challenge == null) {
					return@get call.respond(HttpStatusCode.BadRequest, "Missing headers")
				}

				val valid = TrackingHandler().checkValid(licensee, address, hostname, version, challenge)

				if (!valid) {
					return@get call.nuhUh()
				}

				val success = TrackingHandler().createServer(addon, address, licensee, hostname, version)

				if (!success) {
					return@get call.respond(HttpStatusCode.InternalServerError, "Internal server error")
				}

				val reviews = ReviewHandler.getAllReviewers(addon)

				val userReviews = reviews.filter { it.userId == userId }

				call.respond(userReviews)
			}
		}

	}
}
