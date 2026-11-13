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
import com.fologne.fologneblocker.dns.DohForwarder
import com.fologne.fologneblocker.dns.DohProvider
import com.fologne.fologneblocker.lists.FilterRepository
import com.fologne.fologneblocker.packet.TUN_MTU
import com.fologne.fologneblocker.packet.UdpDnsPacket

class DnsVpnService : VpnService() {
    private val main = Handler(Looper.getMainLooper())
    private var loop: TunDnsLoop? = null
    private var destroyed = false
    private var finalState: Snapshot? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != START || VpnState.current.phase != Phase.STARTING) {
            if (loop == null) stopSelf(startId)
            return START_NOT_STICKY
        }
        try {
            startForeground(1, notification())
            val provider = DohProvider.entries.firstOrNull { it.id == intent.getStringExtra(PROVIDER) }
                ?: DohProvider.CLOUDFLARE
            val forwarder = DohForwarder(provider, ::protect)
            val tunnel = try {
                establishTunnel()
            } catch (error: RuntimeException) {
                forwarder.close()
                throw error
            }
            if (tunnel == null) {
                forwarder.close()
                stopTunnel(Phase.ERROR, Reason.TUNNEL_FAILED)
            } else {
                val filters = FilterRepository.get(this)
                loop = TunDnsLoop(
                    tunnel,
                    forwarder,
                    filters::rules,
                    filters.statistics,
                    { resolved ->
                        main.post {
                            if (!destroyed && VpnState.current.phase == Phase.ACTIVE) {
                                VpnState.set(Phase.ACTIVE, if (resolved) null else Reason.DOH_FAILED)
                            }
                        }
                    },
                    {
                        main.post {
                            if (!destroyed && VpnState.current.phase == Phase.ACTIVE) {
                                stopTunnel(Phase.ERROR, Reason.TUNNEL_FAILED)
                            }
                        }
                    },
                ).also { it.start() }
                VpnState.set(Phase.ACTIVE)
            }
        } catch (_: RuntimeException) {
            stopTunnel(Phase.ERROR, Reason.TUNNEL_FAILED)
        }
        return START_NOT_STICKY
    }

    private fun establishTunnel(): ParcelFileDescriptor? =
        Builder()
            .setSession("FologneBlocker")
            .setMtu(TUN_MTU)
            .addAddress("192.0.2.1", 32)
            .addDnsServer(UdpDnsPacket.VIRTUAL_V4)
            .addRoute(UdpDnsPacket.VIRTUAL_V4, 32)
            .addAddress("2001:db8::1", 128)
            .addDnsServer(UdpDnsPacket.VIRTUAL_V6)
            .addRoute(UdpDnsPacket.VIRTUAL_V6, 128)
            .establish()

    override fun onRevoke() {
        main.post {
            if (!destroyed) stopTunnel(Phase.REVOKED)
        }
    }

    private fun stopTunnel(phase: Phase, reason: Reason? = null) {
        finalState = Snapshot(phase, reason)
        VpnState.set(Phase.STOPPING)
        stopSelf()
    }

    override fun onDestroy() {
        destroyed = true
        loop?.close()
        loop = null
        stopForeground(STOP_FOREGROUND_REMOVE)
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
        val channel = "dns_vpn"
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            checkNotNull(getSystemService(NotificationManager::class.java)).createNotificationChannel(
                NotificationChannel(channel, "VPN local", NotificationManager.IMPORTANCE_LOW),
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
            .setContentText("VPN local de DNS")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    internal companion object {
        const val START = "com.fologne.fologneblocker.START_DNS"
        const val PROVIDER = "doh_provider"
    }
}
