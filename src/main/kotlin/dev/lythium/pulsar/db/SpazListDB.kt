package dev.lythium.pulsar.db

import dev.lythium.pulsar.SpazList
import dev.lythium.pulsar.SpazListUser
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object SpazListDB {
	fun create(gmodstore_id: UUID): Boolean {
		val existingUser = getByID(gmodstore_id)
		if (existingUser != null) {
			return false
		}

		transaction {
			SpazList.insert {
				it[SpazList.gmodstore_id] = gmodstore_id
			}
		}

		return true
	}

	fun get(): List<SpazListUser> {
		val spazListData = transaction {
			SpazList.selectAll().map {
				SpazListUser(
					gmodstore_id = it[SpazList.gmodstore_id],
				)
			}
		}

		return spazListData;
	}

	fun getByID(id: UUID): SpazListUser? {
		val spazListData = transaction {
			SpazList.select {
				SpazList.gmodstore_id eq id
			}.map {
				SpazListUser(
					gmodstore_id = it[SpazList.gmodstore_id],
				)
			}.firstOrNull()
		}

		return spazListData;
	}
}