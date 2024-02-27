package dev.lythium.pulsar

import io.ktor.client.*
import io.ktor.client.engine.jetty.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.eclipse.jetty.util.ssl.SslContextFactory
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
		val httpClient = HttpClient(Jetty) {
			engine {
				sslContextFactory = SslContextFactory.Client()
				clientCacheSize = 12
			}
		}

		val productIdFilter = Addons.get()?.joinToString(",") { it.id.toString() }

		val response: HttpResponse =
			httpClient.request("https://www.gmodstore.com/api/v3/users/${this.gmodstoreId}/purchases") {
				method = HttpMethod.Get
				headers {
					append(HttpHeaders.Authorization, "Bearer " + System.getenv("GMS_API_KEY"))
				}
				url {
					parameters.append("perPage", "100")
					parameters.append("filter[revoked]", "false")
					parameters.append("filter[productId]", "$productIdFilter")
				}
			}

		httpClient.close()

		val responseBody = response.bodyAsText()
		val json = Json { ignoreUnknownKeys = true }
		val addonResponse = json.decodeFromString<PurchaseResponse>(responseBody)

		val ownedAddons = addonResponse.data.map {
			Addons.getAddon(it.productId)
		}.toTypedArray()

		return ownedAddons
	}

	fun getOwnedExternalAddons() {

	}
}

object Users : UUIDTable() {
	val steam_id = long("steam_id")
	val discord_id = long("discord_id")
	val gmodstore_id = uuid("gmodstore_id")
}