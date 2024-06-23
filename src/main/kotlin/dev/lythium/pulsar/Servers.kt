package dev.lythium.pulsar

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.*

data class Server(
	val id: Int,
	val addon: UUID,
	val address: String,
	val licensee: String,
	val hostname: String,
	val version: String
)

object Servers : IntIdTable() {
	val addon = uuid("addon")
	val address = varchar("address", 21)
	val licensee = varchar("licensee", 22)
	val hostname = varchar("hostname", 255)
	val version = varchar("version", 255)
	val createdAt = datetime("created_at")
	val updatedAt = datetime("updated_at")
}