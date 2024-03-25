package dev.lythium.pulsar

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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

	fun get(): Array<Addon>? {
		if (this.addons?.isEmpty() == true || this.addons == null) {
			val client = OkHttpClient.Builder()
				.build()

			val request = Request.Builder()
				.url("https://www.gmodstore.com/api/v3/teams/${Environment.dotenv.get("GMS_TEAM_ID")}/products")
				.addHeader(HttpHeaders.Authorization, "Bearer " + Environment.dotenv.get("GMS_API_KEY"))
				.build()

			val response = client.newCall(request).execute()

			val responseBody = response.body?.string()

			val json = Json { ignoreUnknownKeys = true }
			val addonResponse = json.decodeFromString<AddonResponse>(responseBody!!)

			this.addons = addonResponse.data.map { Addon(it.id, it.name) }.toTypedArray()
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
