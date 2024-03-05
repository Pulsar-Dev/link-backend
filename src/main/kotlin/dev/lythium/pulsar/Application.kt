package dev.lythium.pulsar

import dev.lythium.pulsar.routes.ticketRoutes
import dev.lythium.pulsar.routes.userRoutes
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

object Environment {
	val dotenv = dotenv()
}

suspend fun main(args: Array<String>) {
	println(Paths.get("").toAbsolutePath().toString())


	val connectionString = Environment.dotenv.get("DATABASE_URL")
	val username = Environment.dotenv.get("DATABASE_USERNAME")
	val password = Environment.dotenv.get("DATABASE_PASSWORD")

	if (connectionString == null) {
		println("DATABASE_URL NOT SET!")
		exitProcess(1)
	}
	if (username == null) {
		println("DATABASE_USERNAME NOT SET!")
		exitProcess(1)
	}
	if (password == null) {
		println("DATABASE_PASSWORD NOT SET!")
		exitProcess(1)
	}

	if (Environment.dotenv.get("API_KEY") == null) {
		println("API_KEY NOT SET!")
		exitProcess(1)
	}

	if (Environment.dotenv.get("GMS_API_KEY") == null) {
		println("GMS_API_KEY NOT SET!")
		exitProcess(1)
	}

	if (Environment.dotenv.get("GMS_TEAM_ID") == null) {
		println("GMS_TEAM_ID NOT SET!")
		exitProcess(1)
	}

	Database.connect(connectionString, driver = "org.mariadb.jdbc.Driver", username, password)

	transaction {
		addLogger(StdOutSqlLogger)

		SchemaUtils.create(Users)
	}

	Addons.get()

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

	install(CORS) {
		anyHost()
		allowHeader(HttpHeaders.ContentType)
	}

	intercept(ApplicationCallPipeline.Plugins) {
		val authHeader = call.request.header("Authorization")

		if (authHeader == null) {
			call.respond(HttpStatusCode.Unauthorized, "Unauthorized.")
			return@intercept finish()
		}

		if (authHeader != Environment.dotenv.get("API_KEY")) {
			call.respond(HttpStatusCode.Forbidden, "Forbidden.")
			return@intercept finish()
		}
	}

	routing {
		get("/") {
			call.respondText("Hello World!")
		}
		get("*") {
			call.respondText("404 Not Found", status = HttpStatusCode.NotFound)
		}

		userRoutes()
		ticketRoutes()
	}
}
