package dev.lythium.pulsar

import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {
	override fun serialize(encoder: Encoder, value: UUID) {
		encoder.encodeString(value.toString())
	}

	override fun deserialize(decoder: Decoder): UUID {
		return UUID.fromString(decoder.decodeString())
	}
}

@Serializable
class User(
	@Serializable(with = UUIDSerializer::class) val id: UUID? = null,
	val steamId: Long,
	@Serializable(with = UUIDSerializer::class) val gmodstoreId: UUID,
	val discordId: Long
) {
	suspend fun getOwnedAddons(): Array<Addon?> {
		val client = OkHttpClient.Builder()
			.build()

		val productIdFilter = Addons.get()?.joinToString(",") { it.id.toString() }

		val request = Request.Builder()
			.url("https://www.gmodstore.com/api/v3/users/${this.gmodstoreId}/purchases?perPage=100&filter[revoked]=false&filter[productId]=$productIdFilter")
			.addHeader(HttpHeaders.Authorization, "Bearer " + Environment.dotenv.get("GMS_API_KEY"))
			.build()

		val response = client.newCall(request).execute()

		val responseBody = response.body?.string()

		val json = Json { ignoreUnknownKeys = true }
		val addonResponse = json.decodeFromString<PurchaseResponse>(responseBody!!)

		if (addonResponse.message != null) {
			return arrayOf()
		}

		val ownedAddons = addonResponse.data!!.map {
			Addons.getAddon(it.productId)
		}.toTypedArray()

		return ownedAddons
	}
}

object Users : UUIDTable() {
	val steam_id = long("steam_id")
	val discord_id = long("discord_id")
	val gmodstore_id = uuid("gmodstore_id")
}