package com.fologne.fologneblocker.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import com.fologne.fologneblocker.MainActivity
import java.io.IOException

class DnsVpnService : VpnService() {
    private val main = Handler(Looper.getMainLooper())
    private var tunnel: ParcelFileDescriptor? = null
    private var destroyed = false
    private var finalState: Snapshot? = null
    private val timeout = Runnable { stopDiagnostic(Phase.IDLE, Reason.COMPLETE) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != START || VpnState.current.phase != Phase.STARTING) {
            if (tunnel == null) stopSelf(startId)
            return START_NOT_STICKY
        }
        try {
            startForeground(1, notification())
            // Antes do leitor TUN, só o próprio app usa o DNS virtual.
            tunnel = Builder()
                .setSession("FologneBlocker")
                .addAllowedApplication(packageName)
                .addAddress("192.0.2.1", 32)
                .addDnsServer("192.0.2.2")
                .addRoute("192.0.2.2", 32)
                .addAddress("2001:db8::1", 128)
                .addDnsServer("2001:db8::2")
                .addRoute("2001:db8::2", 128)
                .establish()
            if (tunnel == null) {
                stopDiagnostic(Phase.ERROR, Reason.TUNNEL_FAILED)
            } else {
                VpnState.set(Phase.ACTIVE)
                main.postDelayed(timeout, 30_000L)
            }
        } catch (_: RuntimeException) {
            stopDiagnostic(Phase.ERROR, Reason.TUNNEL_FAILED)
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        main.post {
            if (!destroyed) stopDiagnostic(Phase.REVOKED)
        }
    }

    private fun stopDiagnostic(phase: Phase, reason: Reason? = null) {
        finalState = Snapshot(phase, reason)
        VpnState.set(Phase.STOPPING)
        stopSelf()
    }

    override fun onDestroy() {
        destroyed = true
        main.removeCallbacks(timeout)
        try {
            tunnel?.close()
        } catch (_: IOException) {
            // O sistema pode ter invalidado o descritor durante a revogação.
        } finally {
            tunnel = null
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        val outcome = finalState ?: when (VpnState.current.phase) {
            Phase.STOPPING -> Snapshot(Phase.IDLE)
            Phase.STARTING, Phase.ACTIVE -> Snapshot(Phase.ERROR, Reason.STOPPED)
            else -> VpnState.current
        }
        VpnState.set(outcome.phase, outcome.reason)
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun notification(): Notification {
        val channel = "vpn_diagnostic"
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            checkNotNull(getSystemService(NotificationManager::class.java)).createNotificationChannel(
                NotificationChannel(channel, "Diagnóstico de VPN", NotificationManager.IMPORTANCE_LOW),
            )
            Notification.Builder(this, channel)
        } else {
            Notification.Builder(this)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return builder.setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("FologneBlocker")
            .setContentText("Diagnóstico do túnel local")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    internal companion object {
        const val START = "com.fologne.fologneblocker.START_DIAGNOSTIC"
    }
}
