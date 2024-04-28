package dev.lythium.pulsar

import io.ktor.http.*
import io.ktor.server.application.*
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object Discord {
	fun userInDiscord(user: User): Boolean {
		val client = OkHttpClient.Builder()
			.build()

		val request = Request.Builder()
			.url("https://discord.com/api/v9/guilds/${Environment.dotenv.get("DISCORD_GUILD_ID")}/members/${user.discordId}")
			.addHeader(HttpHeaders.Authorization, "Bot " + Environment.dotenv.get("DISCORD_BOT_TOKEN"))
			.build()

		val response = client.newCall(request).execute()

		val responseBody = response.body?.string()

		responseBody ?: return false

		val json = Json { ignoreUnknownKeys = true }
		val jsonObject = json.parseToJsonElement(responseBody).jsonObject

		return jsonObject["message"] == null
	}

	fun giveUserRole(user: User, role: String) {
		if (!userInDiscord(user)) {
			return
		}


	}
}