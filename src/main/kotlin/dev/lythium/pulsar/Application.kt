package dev.lythium.pulsar

import dev.lythium.pulsar.routes.spazListRoutes
import dev.lythium.pulsar.routes.ticketRoutes
import dev.lythium.pulsar.routes.userRoutes
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

object Environment {
	val dotenv = dotenv()
}

@Serializable
class GmodStoreSpazListObject {
	val steamid:  String? = null
	val user_id: String? = null
	val product_id: String? = null
	val version_name: String? = null
	val extra: String? = null

}

fun main(args: Array<String>) {
	val connectionString = Environment.dotenv.get("DATABASE_URL")
	val username = Environment.dotenv.get("DATABASE_USERNAME")
	val password = Environment.dotenv.get("DATABASE_PASSWORD")

	connectionString ?: run {
		println("DATABASE_URL NOT SET!")
		exitProcess(1)
	}

	username ?: run {
		println("DATABASE_USERNAME NOT SET!")
		exitProcess(1)
	}

	password ?: run {
		println("DATABASE_PASSWORD NOT SET!")
		exitProcess(1)
	}

	Environment.dotenv.get("API_KEY") ?: run {
		println("API_KEY NOT SET!")
		exitProcess(1)
	}

	Environment.dotenv.get("GMS_API_KEY") ?: run {
		println("GMS_API_KEY NOT SET!")
		exitProcess(1)
	}

	Environment.dotenv.get("GMS_TEAM_ID") ?: run {
		println("GMS_TEAM_ID NOT SET!")
		exitProcess(1)
	}

	Database.connect(connectionString, driver = "org.mariadb.jdbc.Driver", username, password)

	transaction {
		addLogger(StdOutSqlLogger)

		SchemaUtils.create(Users)
		SchemaUtils.create(Tickets)
		SchemaUtils.create(TicketMessages)
		SchemaUtils.create(SpazList)
	}

	Addons.get()

	val port = Environment.dotenv["PORT"]?.toInt() ?: 8080
	System.setProperty("ktor.deployment.port", port.toString())

	return io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
	install(ContentNegotiation) {
		json(Json {
			prettyPrint = true
		})
	}

	install(RateLimit) {
		global {
			rateLimiter(limit = 5, refillPeriod = 60.seconds)
		}
	}

	install(Resources)
	install(CallLogging) {
		level = Level.INFO
		format { call ->
			val path = call.request.path()
			val status = call.response.status()
			val httpMethod = call.request.httpMethod.value
			val userAgent = call.request.headers["User-Agent"]
			"Path: $path, Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
		}
	}

	install(CORS) {
		anyHost()
		allowHeader(HttpHeaders.ContentType)
	}

	intercept(ApplicationCallPipeline.Plugins) {
		val authHeader = call.request.header("Authorization")

		if (call.request.path().contains("/spazlist")) {
			val body = call.receiveText()
			val json = Json { ignoreUnknownKeys = true }
			val spazListObject = json.decodeFromString<GmodStoreSpazListObject>(body)

			val secretKey = spazListObject.extra;

			if (secretKey != Environment.dotenv.get("SPAZ_KEY")) {
				call.respond(HttpStatusCode.Forbidden, "Forbidden.")
				return@intercept finish()
			}

			proceed()
			return@intercept
		}

		if (authHeader == null) {
			call.respond(HttpStatusCode.Unauthorized, "Unauthorized.")
			return@intercept finish()
		}

		if (authHeader != Environment.dotenv.get("API_KEY")) {
			call.respond(HttpStatusCode.Forbidden, "Forbidden.")
			return@intercept finish()
		}

		proceed()
	}

	routing {
		get("/") {
			call.respondText("hi")
		}
		get("/addons") {
			val addons = Addons.get()
			call.respond(addons!!)
		}
		get("/addons/{id}") {
			val id =
				call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid addon parameter.")

			val addonId: UUID?
			try {
				addonId = UUID.fromString(id)
			} catch (e: IllegalArgumentException) {
				call.respond(HttpStatusCode.BadRequest, "Invalid addon parameter.")
				return@get
			}

			val addon = Addons.getAddon(addonId) ?: return@get call.respond(HttpStatusCode.NotFound, "Addon not found.")

			call.respond(HttpStatusCode.OK, addon)
		}

		get("*") {
			call.respondText("404 Not Found", status = HttpStatusCode.NotFound)
		}

		userRoutes()
		ticketRoutes()
		spazListRoutes()
	}
}
