package com.fologne.fologneblocker.vpn

internal enum class Phase(val wire: String) {
    IDLE("idle"), PERMISSION("permission_required"), STARTING("starting"),
    ACTIVE("active"), STOPPING("stopping"), ERROR("error"), REVOKED("revoked"),
}

internal enum class Reason(val wire: String) {
    CANCELLED("permission_cancelled"), CONFLICT("vpn_conflict"),
    START_FAILED("start_failed"), TUNNEL_FAILED("tunnel_failed"),
    STOPPED("service_stopped"), DOH_FAILED("doh_failed"),
}

internal data class Snapshot(val phase: Phase, val reason: Reason? = null) {
    fun encode() = mapOf("version" to 1, "state" to phase.wire, "reason" to reason?.wire)
}

// Todas as transições e assinaturas acontecem na thread principal.
internal object VpnState {
    var current = Snapshot(Phase.IDLE)
        private set
    private val listeners = mutableSetOf<(Snapshot) -> Unit>()

    fun set(phase: Phase, reason: Reason? = null) {
        val next = Snapshot(phase, reason)
        if (next == current) return
        current = next
        listeners.toList().forEach { it(next) }
    }

    fun listen(listener: (Snapshot) -> Unit) {
        listeners.add(listener)
        listener(current)
    }

    fun remove(listener: (Snapshot) -> Unit) {
        listeners.remove(listener)
    }
}
