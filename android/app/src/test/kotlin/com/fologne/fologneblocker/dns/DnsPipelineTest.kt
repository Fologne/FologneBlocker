package com.fologne.fologneblocker.dns

import com.fologne.fologneblocker.packet.UdpDnsPacket
import com.fologne.fologneblocker.packet.put16
import com.fologne.fologneblocker.packet.u16
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

class DnsPipelineTest {
    private fun query(type: Int = 1): ByteArray = byteArrayOf(
        0x12, 0x34, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0,
        7, 101, 120, 97, 109, 112, 108, 101, 4, 116, 101, 115, 116, 0,
        0, type.toByte(), 0, 1,
    )

    @Test fun preservesIdTypeAndFlags() {
        for (type in listOf(1, 28, 65)) {
            val parsed = DnsQuery.parse(query(type))!!
            assertEquals(0, parsed.wire.u16(0))
            val answer = parsed.wire.copyOf().apply { put16(2, 0x8183) }
            val restored = parsed.answer(answer, 1232)!!
            assertEquals(0x1234, restored.u16(0))
            assertEquals(0x8183, restored.u16(2))
            assertEquals(type, restored.u16(restored.size - 4))
            assertEquals(0x8182, parsed.failure().u16(2))
            answer.put16(answer.size - 4, 2)
            assertNull(parsed.answer(answer, 1232))
        }
    }

    @Test fun validatesCompressionEdnsAndTruncation() {
        val original = query()
        val edns = original.copyOf(original.size + 11).apply {
            put16(10, 1)
            put16(original.size + 1, 41)
            put16(original.size + 3, 1232)
        }
        val parsed = DnsQuery.parse(edns)!!
        assertEquals(1232, parsed.udpSize)
        assertEquals(1, parsed.failure().u16(10))
        assertNull(DnsQuery.parse(edns.copyOf(edns.size - 1)))
        val response = parsed.wire.copyOf().apply { put16(2, 0x8180) }
        assertEquals(0x8380, parsed.answer(response, original.size)!!.u16(2))
        response.put16(2, 0x8380)
        assertNull(parsed.answer(response, 1232))
        val badPointer = original.copyOf().apply { put16(12, 0xc00c) }
        assertNull(DnsQuery.parse(badPointer))
    }

    @Test fun validatesAndRepliesToIpv4AndIpv6() {
        val original = query()
        for (ipv4 in listOf(true, false)) {
            val header = if (ipv4) 20 else 40
            val packet = ByteArray(header + 8 + original.size)
            val source = InetAddress.getByName(if (ipv4) "192.0.2.1" else "2001:db8::1").address
            val destination = InetAddress.getByName(if (ipv4) "192.0.2.2" else "2001:db8::2").address
            val offset = if (ipv4) 12 else 8
            source.copyInto(packet, offset)
            destination.copyInto(packet, offset + source.size)
            val length = packet.size - header
            if (ipv4) {
                packet[0] = 0x45
                packet[8] = 64
                packet[9] = 17
                packet.put16(2, packet.size)
                packet.put16(10, checksum(packet.copyOf(header)))
            } else {
                packet[0] = 0x60
                packet[6] = 17
                packet[7] = 64
                packet.put16(4, length)
            }
            packet.put16(header, 43123)
            packet.put16(header + 2, 53)
            packet.put16(header + 4, length)
            original.copyInto(packet, header + 8)
            val tail = if (ipv4) byteArrayOf(0, 17, 0, length.toByte())
                else byteArrayOf(0, 0, 0, length.toByte(), 0, 0, 0, 17)
            packet.put16(header + 6, checksum(source + destination + tail + packet.copyOfRange(header, packet.size)))
            val request = UdpDnsPacket.read(packet, packet.size)!!
            assertArrayEquals(original, request.message)
            val response = request.reply(original)
            assertEquals(53, response.u16(header))
            assertEquals(43123, response.u16(header + 2))
            assertEquals(0, checksum(destination + source + tail + response.copyOfRange(header, response.size)))
            if (ipv4) assertEquals(0, checksum(response.copyOf(header)))
            packet[packet.lastIndex] = 9
            assertNull(UdpDnsPacket.read(packet, packet.size))
        }
    }

    @Test fun rejectsSocketWhenVpnProtectionFailsAndRejectsCallsAfterClose() {
        var attempts = 0
        val resolver = DohForwarder(DohProvider.CLOUDFLARE) {
            attempts++
            assertFalse(it.isConnected)
            false
        }
        assertThrows(IOException::class.java) { resolver.resolve(query()) }
        assertTrue(attempts > 0)
        resolver.close()
        val previousAttempts = attempts
        assertThrows(IOException::class.java) { resolver.resolve(query()) }
        assertEquals(previousAttempts, attempts)
    }

    private fun checksum(bytes: ByteArray): Int {
        var value = 0
        for (i in bytes.indices) {
            value += (bytes[i].toInt() and 255) shl (if (i % 2 == 0) 8 else 0)
            if (value > 65535) value -= 65535
        }
        return 65535 - value
    }
}
