package dev.lythium.pulsar.routes

import dev.lythium.pulsar.db.UserDB
import dev.lythium.pulsar.getUUID
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID
import kotlin.reflect.typeOf

fun Route.userRoutes() {
    route("/user") {
        post {
            if (call.request.header("Authorization") != System.getenv("API_KEY")) {
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized.")
                return@post
            }

            val steamId = call.parameters["steamId"]?.toLong()
            val discordId = call.parameters["discordId"]?.toLong()
            val gmodstoreId = UUID.fromString(call.parameters["gmodstoreId"])

            //println(steamId, discordId, gmodstoreId)

            if (steamId == null || discordId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing parameters.")
                return@post
            }

            val success = UserDB.create(steamId, discordId, gmodstoreId);

            if (!success) {
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error.")
                return@post
            }

            call.respondText("User created")
        }
    }
}