package dev.lythium.pulsar.db

import dev.lythium.pulsar.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object UserDB {
    fun create(steamId: Long, discordId: Long, gmodstoreId: UUID): Boolean {
        transaction {
            User.insert {
                it[steam_id] = steamId
                it[discord_id] = discordId
                it[gmodstore_id] = gmodstoreId
            }
        }

        return true;
    }
}