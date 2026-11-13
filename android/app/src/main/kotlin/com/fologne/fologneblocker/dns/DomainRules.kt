package com.fologne.fologneblocker.dns

import com.fologne.fologneblocker.packet.u8
import java.net.IDN
import java.util.Locale

internal fun normalizeDomain(value: String): String? {
    val name = value.trim().removeSuffix(".")
    if (name.isEmpty() || name.length > 253) return null
    val ascii = try {
        IDN.toASCII(name, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return ascii.takeIf {
        it.length <= 253 && !it.startsWith('.') && !it.endsWith('.') && !it.contains("..")
    }
}

internal fun domainFromWire(wire: ByteArray): String? {
    val name = StringBuilder()
    var offset = 0
    while (wire.u8(offset) != 0) {
        val length = wire.u8(offset++)
        if (name.isNotEmpty()) name.append('.')
        repeat(length) {
            val ch = wire.u8(offset++).toChar()
            if (ch !in 'a'..'z' && ch !in 'A'..'Z' && ch !in '0'..'9' && ch != '-') return null
            name.append(ch)
        }
    }
    return normalizeDomain(name.toString())
}

internal class RuleSnapshot(
    allow: Set<String> = emptySet(),
    block: Set<String> = emptySet(),
    sources: List<Set<String>> = emptyList(),
) {
    private val allow = allow.toSet()
    private val block = block.toSet()
    private val sources = sources.toList()

    // Os conjuntos remotos pertencem ao parser e nunca são modificados após a publicação.
    fun blocks(domain: String): Boolean =
        !allow.matches(domain) && (block.matches(domain) || sources.any { it.matches(domain) })

    private fun Set<String>.matches(domain: String): Boolean {
        if (isEmpty()) return false
        var suffix = domain
        while (true) {
            if (suffix in this) return true
            val dot = suffix.indexOf('.')
            if (dot == -1) return false
            suffix = suffix.substring(dot + 1)
        }
    }
}

internal class DnsStatistics {
    private var allowed = 0L
    private var blocked = 0L
    private var errors = 0L

    @Synchronized
    fun record(blockedLocally: Boolean, success: Boolean) {
        when {
            !success -> errors++
            blockedLocally -> blocked++
            else -> allowed++
        }
    }

    @Synchronized
    fun snapshot(): Map<String, Long> = mapOf("allowed" to allowed, "blocked" to blocked, "errors" to errors)
}
