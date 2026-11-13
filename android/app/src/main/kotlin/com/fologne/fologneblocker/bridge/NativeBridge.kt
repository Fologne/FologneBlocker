package com.fologne.fologneblocker.bridge

import com.fologne.fologneblocker.vpn.Snapshot
import com.fologne.fologneblocker.vpn.VpnState
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

internal class NativeBridge(messenger: BinaryMessenger, start: (String) -> Unit, stop: () -> Unit) {
    private val methods = MethodChannel(messenger, "com.fologne.fologneblocker/vpn")
    private val events = EventChannel(messenger, "com.fologne.fologneblocker/vpn_state")
    private var sink: EventChannel.EventSink? = null
    private val listener: (Snapshot) -> Unit = { sink?.success(it.encode()) }

    init {
        methods.setMethodCallHandler { call, result ->
            when (call.method) {
                "getProtectionState" -> result.success(VpnState.current.encode())
                "startProtection" -> {
                    start(call.argument<String>("provider") ?: "cloudflare")
                    result.success(null)
                }
                "stopProtection" -> {
                    stop()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
        events.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink) {
                sink = eventSink
                VpnState.listen(listener)
            }

            override fun onCancel(arguments: Any?) = detach()
        })
    }

    fun close() {
        detach()
        methods.setMethodCallHandler(null)
        events.setStreamHandler(null)
    }

    private fun detach() {
        VpnState.remove(listener)
        sink = null
    }
}
