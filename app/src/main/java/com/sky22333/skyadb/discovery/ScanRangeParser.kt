package com.sky22333.skyadb.discovery

import java.net.InetAddress

object ScanRangeParser {
    fun parseConfiguredRanges(value: String): List<LocalNetwork> {
        return value
            .split("\n", ",", "，", ";", "；")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { parseEntry(it, sourceLabel = "手动配置") }
            .distinctBy { it.subnetLabel }
            .take(MaxConfiguredRanges)
    }

    fun validationError(value: String): String? {
        val entries = value
            .split("\n", ",", "，", ";", "；")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (entries.size > MaxConfiguredRanges) return "最多配置 $MaxConfiguredRanges 个网段"
        val invalid = entries.firstOrNull { parseEntry(it, sourceLabel = "手动配置") == null }
        return if (invalid == null) null else "格式错误或范围过大：$invalid"
    }

    fun subnetForHost(host: String, sourceLabel: String): LocalNetwork? {
        return if (host.isPrivateIpv4()) {
            createSubnet(address = host, prefixLength = 24, sourceLabel = sourceLabel, excludedHost = null)
        } else {
            null
        }
    }

    fun subnetForLocalAddress(address: String, sourceLabel: String): LocalNetwork? {
        if (!address.isPrivateIpv4()) return null
        return createSubnet(address = address, prefixLength = DefaultPrefixLength, sourceLabel = sourceLabel)
    }

    private fun parseEntry(entry: String, sourceLabel: String): LocalNetwork? {
        val parts = entry.split("/")
        val address = parts.firstOrNull()?.trim().orEmpty()
        if (!address.isPrivateIpv4()) return null
        val prefix = when (parts.size) {
            1 -> DefaultPrefixLength
            2 -> parts[1].toIntOrNull() ?: return null
            else -> return null
        }
        if (prefix !in 24..32) return null
        return createSubnet(address = address, prefixLength = prefix, sourceLabel = sourceLabel, excludedHost = null)
    }

    private fun createSubnet(
        address: String,
        prefixLength: Int,
        sourceLabel: String,
        excludedHost: String? = address,
    ): LocalNetwork {
        val addressInt = address.toIpv4Int()
        val mask = prefixLength.toMask()
        val network = addressInt and mask
        val broadcast = network or mask.inv()
        val hosts = when (prefixLength) {
            32 -> listOf(address)
            31 -> listOf(network.toIpv4String(), broadcast.toIpv4String())
            else -> ((network + 1)..(broadcast - 1)).map { it.toIpv4String() }
        }.filterNot { it == excludedHost }

        return LocalNetwork(
            deviceIp = address,
            subnetLabel = "${network.toIpv4String()}/$prefixLength",
            hosts = hosts,
            sourceLabel = sourceLabel,
        )
    }

    private fun String.isPrivateIpv4(): Boolean {
        val parts = split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        return parts[0] == 10 ||
            parts[0] == 192 && parts[1] == 168 ||
            parts[0] == 172 && parts[1] in 16..31
    }

    private fun String.toIpv4Int(): Int {
        val bytes = InetAddress.getByName(this).address
        return bytes.fold(0) { value, byte -> (value shl 8) or (byte.toInt() and 0xff) }
    }

    private fun Int.toIpv4String(): String {
        return listOf(
            this ushr 24 and 0xff,
            this ushr 16 and 0xff,
            this ushr 8 and 0xff,
            this and 0xff,
        ).joinToString(".")
    }

    private fun Int.toMask(): Int {
        return if (this == 0) 0 else -1 shl (32 - this)
    }

    private const val MaxConfiguredRanges = 6
    private const val DefaultPrefixLength = 24
}
