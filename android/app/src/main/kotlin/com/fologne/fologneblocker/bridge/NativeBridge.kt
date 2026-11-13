package com.fologne.fologneblocker.bridge

import android.os.Handler
import android.os.Looper
import com.fologne.fologneblocker.lists.FilterRepository
import com.fologne.fologneblocker.lists.ListSource
import com.fologne.fologneblocker.vpn.Snapshot
import com.fologne.fologneblocker.vpn.VpnState
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.Executors

internal class NativeBridge(
    messenger: BinaryMessenger,
    start: (String) -> Unit,
    stop: () -> Unit,
    private val filters: FilterRepository,
) {
    private val main = Handler(Looper.getMainLooper())
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
                "getStatistics" -> result.success(filters.statistics.snapshot())
                "getFilters", "setFilter", "saveRule", "deleteRule", "updateList" ->
                    manageFilters(call, result)
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

    private fun manageFilters(call: MethodCall, result: MethodChannel.Result) {
        val executor = if (call.method == "updateList") updates else settings
        executor.execute {
            try {
                when (call.method) {
                    "setFilter" -> filters.setEnabled(
                        ListSource.valueOf(requireNotNull(call.argument<String>("id"))),
                        requireNotNull(call.argument<Boolean>("enabled")),
                    )
                    "saveRule" -> filters.saveRule(
                        call.argument<String>("previous"),
                        requireNotNull(call.argument<String>("domain")),
                        requireNotNull(call.argument<Boolean>("allow")),
                    )
                    "deleteRule" -> filters.deleteRule(requireNotNull(call.argument<String>("domain")))
                    "updateList" -> filters.refresh(ListSource.valueOf(requireNotNull(call.argument<String>("id"))))
                }
                val state = filters.state()
                main.post { result.success(state) }
            } catch (_: IllegalArgumentException) {
                main.post { result.error("invalid_filter", "Confira o domínio e os dados da regra.", null) }
            } catch (_: Exception) {
                main.post { result.error("filter_failed", "Não foi possível acessar ou atualizar os filtros.", null) }
            }
        }
    }

    private companion object {
        val settings = Executors.newSingleThreadExecutor()
        val updates = Executors.newSingleThreadExecutor()
    }

    private fun detach() {
        VpnState.remove(listener)
        sink = null
    }
}
