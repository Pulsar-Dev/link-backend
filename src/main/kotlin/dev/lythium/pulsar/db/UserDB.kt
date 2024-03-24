package dev.lythium.pulsar.db

import dev.lythium.pulsar.User
import dev.lythium.pulsar.Users
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserExistsException : Exception("User already exists.")

object UserDB {
	fun create(steamId: Long, discordId: Long, gmodstoreId: UUID): UUID {
		var exists = false

		exists = exists || get(steamId = steamId) != null
		exists = exists || get(discordId = discordId) != null
		exists = exists || get(gmodstoreId = gmodstoreId) != null

		if (exists) throw UserExistsException()

		val insertedId = transaction {
			Users.insertAndGetId {
				it[steam_id] = steamId
				it[discord_id] = discordId
				it[gmodstore_id] = gmodstoreId
			}.value
		}

		return insertedId
	}

	fun get(id: UUID? = null, steamId: Long? = null, discordId: Long? = null, gmodstoreId: UUID? = null): User? {
		if (id == null && steamId == null && discordId == null && gmodstoreId == null) return null

		val userData = transaction {
			Users.select {
				if (id != null) Users.id eq id
				else if (steamId != null) Users.steam_id eq steamId
				else if (discordId != null) Users.discord_id eq discordId
				else if (gmodstoreId != null) Users.gmodstore_id eq gmodstoreId
				else return@transaction 0
			}.map {
				User(
					id = it[Users.id].value,
					steamId = it[Users.steam_id],
					discordId = it[Users.discord_id],
					gmodstoreId = it[Users.gmodstore_id]
				)
			}.firstOrNull()
		}

		if (userData !is User) return null

		return userData
	}

	fun exists(id: UUID? = null, steamId: Long? = null, discordId: Long? = null, gmodstoreId: UUID? = null): Boolean {
		return get(id, steamId, discordId, gmodstoreId) != null
	}
}