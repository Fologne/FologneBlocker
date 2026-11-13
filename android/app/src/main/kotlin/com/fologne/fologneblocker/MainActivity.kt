package com.fologne.fologneblocker

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import com.fologne.fologneblocker.bridge.NativeBridge
import com.fologne.fologneblocker.lists.FilterRepository
import com.fologne.fologneblocker.lists.ListUpdateWorker
import com.fologne.fologneblocker.vpn.DnsVpnService
import com.fologne.fologneblocker.vpn.Phase
import com.fologne.fologneblocker.vpn.Reason
import com.fologne.fologneblocker.vpn.VpnState
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    private var bridge: NativeBridge? = null
    private var awaitingConsent = false
    private var provider = "cloudflare"

    override fun onCreate(savedInstanceState: Bundle?) {
        awaitingConsent = savedInstanceState?.getBoolean("vpn_consent_pending") == true
        provider = savedInstanceState?.getString("doh_provider") ?: "cloudflare"
        if (awaitingConsent && VpnState.current.phase == Phase.IDLE) {
            VpnState.set(Phase.PERMISSION)
        }
        super.onCreate(savedInstanceState)
        ListUpdateWorker.schedule(applicationContext)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("vpn_consent_pending", awaitingConsent)
        outState.putString("doh_provider", provider)
        super.onSaveInstanceState(outState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        bridge = NativeBridge(
            flutterEngine.dartExecutor.binaryMessenger, ::startVpn, ::stopVpn, FilterRepository.get(this),
        )
    }

    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        bridge?.close()
        bridge = null
        super.cleanUpFlutterEngine(flutterEngine)
    }

    @Suppress("DEPRECATION")
    private fun startVpn(selectedProvider: String) {
        when (VpnState.current.phase) {
            Phase.IDLE, Phase.ERROR, Phase.REVOKED -> Unit
            else -> return
        }
        provider = selectedProvider
        try {
            val connectivity = getSystemService(ConnectivityManager::class.java)
            val active = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
            if (active?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                VpnState.set(Phase.ERROR, Reason.CONFLICT)
                return
            }
            val consent = VpnService.prepare(this)
            if (consent == null) {
                launchService()
            } else {
                awaitingConsent = true
                VpnState.set(Phase.PERMISSION)
                startActivityForResult(consent, VPN_REQUEST)
            }
        } catch (_: RuntimeException) {
            awaitingConsent = false
            VpnState.set(Phase.ERROR, Reason.START_FAILED)
        }
    }

    @Deprecated("Resultado da autorização no FlutterActivity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != VPN_REQUEST || !awaitingConsent) return
        awaitingConsent = false
        if (resultCode == RESULT_OK) {
            launchService()
        } else {
            VpnState.set(Phase.IDLE, Reason.CANCELLED)
        }
    }

    private fun launchService() {
        VpnState.set(Phase.STARTING)
        try {
            val request = Intent(this, DnsVpnService::class.java)
                .setAction(DnsVpnService.START)
                .putExtra(DnsVpnService.PROVIDER, provider)
            val service = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(request)
            } else {
                startService(request)
            }
            if (service == null) VpnState.set(Phase.ERROR, Reason.START_FAILED)
        } catch (_: RuntimeException) {
            VpnState.set(Phase.ERROR, Reason.START_FAILED)
        }
    }

    private fun stopVpn() {
        if (VpnState.current.phase != Phase.ACTIVE) return
        VpnState.set(Phase.STOPPING)
        if (!stopService(Intent(this, DnsVpnService::class.java))) {
            VpnState.set(Phase.IDLE)
        }
    }

    private companion object {
        const val VPN_REQUEST = 701
    }
}
