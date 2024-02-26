package dev.lythium.pulsar

import dev.lythium.pulsar.routes.userRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.system.exitProcess

fun getUUID(): UUID {
    return UUID.randomUUID()
}

fun main(args: Array<String>) {
    var connectionString = System.getenv("DATABASE_URL")
    var username = System.getenv("DATABASE_USERNAME")
    var password = System.getenv("DATABASE_PASSWORD")

    if (connectionString == null) {
        println("DATABASE_URL NOT SET!");
        exitProcess(1);
    }
    if (username == null) {
        println("DATABASE_USERNAME NOT SET!");
        exitProcess(1);
    };
    if (password == null) {
        println("DATABASE_PASSWORD NOT SET!");
        exitProcess(1);
    };

    if (System.getenv("API_KEY") == null) {
        println("API_KEY NOT SET!")
        exitProcess(1)
    }

    Database.connect(connectionString, driver = "org.mariadb.jdbc.Driver", username, password)

    transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.create(User)
    }

    return io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(Routing) {
        userRoutes()
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
