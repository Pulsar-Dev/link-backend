package dev.lythium.pulsar

import dev.lythium.pulsar.routes.ticketRoutes
import dev.lythium.pulsar.routes.userRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.system.exitProcess

suspend fun main(args: Array<String>) {
	val connectionString = System.getenv("DATABASE_URL")
	val username = System.getenv("DATABASE_USERNAME")
	val password = System.getenv("DATABASE_PASSWORD")

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

	if (System.getenv("API_KEY") == null) {
		println("API_KEY NOT SET!")
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

	install(CORS) {
		anyHost()
		allowHeader(HttpHeaders.ContentType)
	}

	install(Routing) {
		userRoutes()
		ticketRoutes()
	}

	routing {
		get("/") {
			call.respondText("Hello World!")
		}
		get("*") {
			call.respondText("404 Not Found", status = HttpStatusCode.NotFound)
		}

	}
}
