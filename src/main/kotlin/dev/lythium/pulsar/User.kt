package dev.lythium.pulsar

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable

//@Serializable
//class User(val id: String = getUUID(), val steamId: String, val gmodstoreId: String, val discordId: String) {
//}

object User: UUIDTable() {
    val steam_id = long("steam_id")
    val discord_id = long("discord_id")
    val gmodstore_id = uuid("gmodstore_id")
}

