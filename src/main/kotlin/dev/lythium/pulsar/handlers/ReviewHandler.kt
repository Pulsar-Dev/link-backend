package dev.lythium.pulsar.handlers

import dev.lythium.pulsar.Environment
import dev.lythium.pulsar.UUIDSerializer
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

@Serializable
class AddonReviewResponse(
	val data: Array<AddonReview>
)

@Serializable
class AddonReview(
	@Serializable(with = UUIDSerializer::class) val id: UUID? = null,
	@Serializable(with = UUIDSerializer::class) val userId: UUID? = null,
	val title: String? = null,
	val body: String? = null,
	val rating: Int? = null
)

object ReviewHandler {
	fun getAllReviewers(addon: UUID): Array<AddonReview> {
		val client = OkHttpClient.Builder()
			.build()

		val request = Request.Builder()
			.url("https://www.gmodstore.com/api/v3/products/${addon}/reviews")
			.addHeader(HttpHeaders.Authorization, "Bearer " + Environment.dotenv.get("GMS_API_KEY"))
			.build()

		val response = client.newCall(request).execute()

		val responseBody = response.body?.string()

		val json = Json { ignoreUnknownKeys = true }
		val addonResponse = json.decodeFromString<AddonReviewResponse>(responseBody!!)

		return addonResponse.data
	}
}