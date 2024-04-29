package dev.lythium.pulsar.handlers

import dev.lythium.pulsar.Server
import dev.lythium.pulsar.Servers
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

class TrackingHandler {
	fun checkValid(licensee: String, address: String, hostname: String, version: String, challenge: String): Boolean {
		if (licensee.isBlank() || address.isBlank() || hostname.isBlank() || version.isBlank() || challenge.isBlank()) {
			return false
		}

		if (!licensee.all { it.isDigit() } || !challenge.all { it.isDigit() }) {
			return false
		}

		if (licensee == "{{ user_id }}" || address == "{{ address }}" || hostname == "{{ hostname }}" || version == "{{ version }}" || challenge == "{{ user_id | 80 }}") {
			return true
		}

		val licenseeXor = licensee.toLong() xor 0x50L

		if (challenge.toLong() != (licenseeXor)) {
			return false
		}

		if (address == "loopback") {
			return true
		}

		val splitAddress = address.split(":")

		if (splitAddress.size != 2) {
			return false
		}

		val ip = splitAddress[0]
		val splitIp = ip.split(".")
		val port = splitAddress[1]

		if (!port.all { it.isDigit() }) {
			return false
		}

		for (i in splitIp) {
			if (i.toInt() !in 0..255) {
				return false
			}
		}

		if (ip == "255.255.255.255" || ip == "0.0.0.0" || splitIp.size != 4 || port.toInt() !in 1024..65535) {
			return false
		}

		val hostnameLen = hostname.length
		if (hostnameLen < 3 || hostnameLen > 255) {
			return false
		}

		val versionRegex =
			"^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(-(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(\\.(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\\+[0-9a-zA-Z-]+(\\.[0-9a-zA-Z-]+)*)?$".toRegex()
		if (!versionRegex.matches(version)) {
			return false
		}

		return true
	}

	private fun getServer(addon: UUID, address: String, licensee: String): Server? {
		val server = transaction {
			Servers.select {
				Servers.addon eq addon and (Servers.address eq address) and (Servers.licensee eq licensee)
			}.map {
				Server(
					id = it[Servers.id].value,
					addon = it[Servers.addon],
					address = it[Servers.address],
					licensee = it[Servers.licensee],
					hostname = it[Servers.hostname],
					version = it[Servers.version]
				)
			}.firstOrNull()
		}

		println(server)
		return server
	}

	private fun updateServer(
		addon: UUID,
		address: String,
		licensee: String,
		hostname: String,
		version: String
	): Boolean {
		val server = getServer(addon, address, licensee) ?: return false

		transaction {
			Servers.update({ Servers.id eq server.id }) {
				it[Servers.hostname] = hostname
				it[Servers.version] = version
				it[updatedAt] = java.time.LocalDateTime.now()
			}
		}

		return true
	}

	fun createServer(addon: UUID, address: String, licensee: String, hostname: String, version: String): Boolean {
		val serverExists = getServer(addon, address, licensee)

		if (serverExists != null) {
			return updateServer(addon, address, licensee, hostname, version)
		}

		transaction {
			Servers.insert {
				it[Servers.addon] = addon
				it[Servers.address] = address
				it[Servers.licensee] = licensee
				it[Servers.hostname] = hostname
				it[Servers.version] = version
				it[createdAt] = java.time.LocalDateTime.now()
				it[updatedAt] = java.time.LocalDateTime.now()
			}
		}

		return true
	}
}