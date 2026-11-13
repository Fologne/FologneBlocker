package com.fologne.fologneblocker.vpn

import android.database.sqlite.SQLiteException
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.fologne.fologneblocker.dns.DnsQuery
import com.fologne.fologneblocker.dns.DnsStatistics
import com.fologne.fologneblocker.dns.RuleSnapshot
import com.fologne.fologneblocker.dns.DohForwarder
import com.fologne.fologneblocker.packet.TUN_MTU
import com.fologne.fologneblocker.packet.UdpDnsPacket
import com.fologne.fologneblocker.packet.u8
import java.io.IOException

internal class TunDnsLoop(
    private val tunnel: ParcelFileDescriptor,
    private val forwarder: DohForwarder,
    private val rules: () -> RuleSnapshot,
    private val statistics: DnsStatistics,
    private val onResolution: (Boolean) -> Unit,
    private val onTunnelFailure: () -> Unit,
) : AutoCloseable {
    @Volatile private var closed = false
    private val worker = Thread(::run, "FologneBlocker DNS")

    fun start() = worker.start()

    override fun close() {
        closed = true
        // O Android mantém o serviço vinculado enquanto o TUN estiver aberto.
        try {
            tunnel.close()
        } catch (_: IOException) {
            // O descritor pode já ter sido fechado pela thread de leitura.
        }
        worker.interrupt()
    }

    private fun run() {
        try {
            tunnel.use { process() }
        } catch (_: IOException) {
            if (!closed) onTunnelFailure()
        } catch (_: ErrnoException) {
            if (!closed) onTunnelFailure()
        } catch (_: SQLiteException) {
            if (!closed) onTunnelFailure()
        } finally {
            // O fechamento TLS pode escrever na rede e deve ocorrer fora da thread principal.
            forwarder.close()
        }
    }

    private fun process() {
        val descriptor = tunnel.fileDescriptor
        val poll = StructPollfd().apply {
            fd = descriptor
            events = OsConstants.POLLIN.toShort()
        }
        val polls = arrayOf(poll)
        val buffer = ByteArray(TUN_MTU + 1)
        var lastResolution: Boolean? = null
        while (!closed) {
            val ready = try {
                Os.poll(polls, 250)
            } catch (error: ErrnoException) {
                if (error.errno == OsConstants.EINTR) continue
                throw error
            }
            if (ready == 0 || closed) continue
            if (poll.revents.toInt() and OsConstants.POLLIN == 0) {
                throw IOException("Túnel indisponível")
            }
            val size = try {
                Os.read(descriptor, buffer, 0, buffer.size)
            } catch (error: ErrnoException) {
                if (error.errno == OsConstants.EAGAIN || error.errno == OsConstants.EINTR) continue
                throw error
            }
            if (size == 0) throw IOException("Túnel encerrado")
            val packet = UdpDnsPacket.read(buffer, size) ?: continue
            val query = DnsQuery.parse(packet.message) ?: continue
            val blocked = query.domain()?.let { rules().blocks(it) } == true
            val response = if (blocked) query.blocked() else try {
                query.answer(forwarder.resolve(query.wire), packet.maxResponseSize)
            } catch (_: IOException) {
                null
            }
            if (closed) break
            val code = response?.u8(3)?.and(15)
            val success = code == 0 || code == 3
            if (!blocked && lastResolution != success) {
                lastResolution = success
                onResolution(success)
            }
            val output = packet.reply(response ?: query.failure())
            try {
                if (Os.write(descriptor, output, 0, output.size) != output.size) {
                    throw IOException("Escrita incompleta no túnel")
                }
                statistics.record(blocked, success)
            } catch (error: ErrnoException) {
                if (error.errno != OsConstants.EAGAIN && error.errno != OsConstants.EINTR) throw error
            }
        }
    }
}
