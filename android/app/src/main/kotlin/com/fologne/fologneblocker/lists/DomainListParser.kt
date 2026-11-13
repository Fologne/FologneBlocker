package com.fologne.fologneblocker.lists

import com.fologne.fologneblocker.dns.normalizeDomain
import java.io.IOException

internal const val MAX_LIST_BYTES = 16 * 1024 * 1024
private const val MAX_RULES = 500_000

internal fun parseDomainList(text: String): Set<String> {
    if (text.length > MAX_LIST_BYTES) throw IOException("Lista muito grande")
    val domains = HashSet<String>()
    var entries = 0
    var expected: Int? = null
    for (line in text.lineSequence()) {
        val value = line.trim()
        if (value.isEmpty()) continue
        if (value.startsWith('#')) {
            if (value.startsWith("# Number of entries:")) {
                expected = value.substringAfter(':').trim().toIntOrNull()
                    ?: throw IOException("Contagem de regras inválida")
            }
            continue
        }
        if (++entries > MAX_RULES) throw IOException("Lista muito grande")
        domains.add(normalizeDomain(value) ?: throw IOException("Regra inválida"))
    }
    if (domains.isEmpty() || (expected != null && expected != entries)) {
        throw IOException("Lista vazia ou incompleta")
    }
    return domains
}
