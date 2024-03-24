package dev.lythium.pulsar

import io.ktor.client.*
import io.ktor.client.engine.jetty.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.util.*

@Serializable
data class AddonResponse(
	val data: List<AddonData>
)

@Serializable
data class AddonData(
	@Serializable(with = UUIDSerializer::class) val id: UUID,
	val name: String,
)

@Serializable
data class PurchaseResponse(
	val data: List<PurchaseData>
)

@Serializable
data class PurchaseData(
	@Serializable(with = UUIDSerializer::class) val id: UUID,
	@Serializable(with = UUIDSerializer::class) val productId: UUID,
	val revoked: Boolean
)

object Addons {
	private var addons: Array<Addon>? = null

	suspend fun get(): Array<Addon>? {
		if (this.addons?.isEmpty() == true || this.addons == null) {
			val httpClient = HttpClient(Jetty) {
				engine {
					sslContextFactory = SslContextFactory.Client()
					clientCacheSize = 12
				}
			}

			val response: HttpResponse =
				httpClient.request("https://www.gmodstore.com/api/v3/teams/${Environment.dotenv.get("GMS_TEAM_ID")}/products") {
					method = HttpMethod.Get
					headers {
						append(HttpHeaders.Authorization, "Bearer " + Environment.dotenv.get("GMS_API_KEY"))
					}
					parameters {
						append("perPage", "100")
					}
				}

			val responseBody = response.bodyAsText()

			val json = Json { ignoreUnknownKeys = true }
			val addonResponse = json.decodeFromString<AddonResponse>(responseBody)

			this.addons = addonResponse.data.map { Addon(it.id, it.name) }.toTypedArray()

			httpClient.close()
		}

		return this.addons
	}

	suspend fun getAddon(id: UUID): Addon? {
		return this.get()?.find { it.id == id }
	}
}

@Serializable
data class Addon(
	@Serializable(with = UUIDSerializer::class) val id: UUID, val name: String
) {}
