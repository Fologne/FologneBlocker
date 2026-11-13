package com.fologne.fologneblocker.packet

internal const val TUN_MTU = 1280

internal fun ByteArray.u8(offset: Int) = this[offset].toInt() and 255
internal fun ByteArray.u16(offset: Int) = (u8(offset) shl 8) or u8(offset + 1)
internal fun ByteArray.put16(offset: Int, value: Int) {
    this[offset] = (value ushr 8).toByte()
    this[offset + 1] = value.toByte()
}

internal class UdpDnsPacket private constructor(
    private val addresses: ByteArray,
    private val port: Int,
    val message: ByteArray,
) {
    val maxResponseSize get() = TUN_MTU - (if (addresses.size == 8) 20 else 40) - 8

    fun reply(dns: ByteArray): ByteArray {
        require(dns.size <= maxResponseSize)
        val ipv4 = addresses.size == 8
        val header = if (ipv4) 20 else 40
        val addressSize = addresses.size / 2
        val packet = ByteArray(header + 8 + dns.size)
        val addressOffset = if (ipv4) 12 else 8
        addresses.copyInto(packet, addressOffset, addressSize)
        addresses.copyInto(packet, addressOffset + addressSize, 0, addressSize)
        if (ipv4) {
            packet[0] = 0x45
            packet.put16(2, packet.size)
            packet[8] = 64
            packet[9] = 17
            packet.put16(10, checksum(sum(packet, 0, header)))
        } else {
            packet[0] = 0x60
            packet.put16(4, packet.size - header)
            packet[6] = 17
            packet[7] = 64
        }
        val length = packet.size - header
        packet.put16(header, 53)
        packet.put16(header + 2, port)
        packet.put16(header + 4, length)
        dns.copyInto(packet, header + 8)
        val pseudo = sum(packet, addressOffset, addresses.size) + 17 + length
        val udpChecksum = checksum(pseudo + sum(packet, header, length))
        packet.put16(header + 6, if (udpChecksum == 0) 0xffff else udpChecksum)
        return packet
    }

    companion object {
        const val VIRTUAL_V4 = "192.0.2.2"
        const val VIRTUAL_V6 = "2001:db8::2"
        private val ipv4Dns = byteArrayOf(192.toByte(), 0, 2, 2)
        private val ipv6Dns = byteArrayOf(0x20, 1, 0x0d, 0xb8.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2)

        fun read(packet: ByteArray, size: Int): UdpDnsPacket? {
            if (size !in 28..minOf(packet.size, TUN_MTU)) return null
            val version = packet.u8(0) ushr 4
            val header: Int
            val offset: Int
            val dns: ByteArray
            when (version) {
                4 -> {
                    header = (packet.u8(0) and 15) * 4
                    if (header != 20 || packet.u16(2) != size || packet.u8(9) != 17 ||
                        packet.u16(6) and 0xbfff != 0 || checksum(sum(packet, 0, header)) != 0
                    ) return null
                    offset = 12
                    dns = ipv4Dns
                }
                6 -> {
                    header = 40
                    if (size < header + 8 || packet.u16(4) != size - header || packet.u8(6) != 17) return null
                    offset = 8
                    dns = ipv6Dns
                }
                else -> return null
            }
            if (dns.indices.any { packet[offset + dns.size + it] != dns[it] }) return null
            val length = size - header
            val sourcePort = packet.u16(header)
            if (length < 20 || sourcePort == 0 || packet.u16(header + 2) != 53 ||
                packet.u16(header + 4) != length
            ) return null
            val addresses = packet.copyOfRange(offset, offset + dns.size * 2)
            val suppliedChecksum = packet.u16(header + 6)
            if (version == 6 || suppliedChecksum != 0) {
                val pseudo = sum(addresses, 0, addresses.size) + 17 + length
                if (suppliedChecksum == 0 || checksum(pseudo + sum(packet, header, length)) != 0) return null
            }
            return UdpDnsPacket(addresses, sourcePort, packet.copyOfRange(header + 8, size))
        }

        private fun sum(bytes: ByteArray, start: Int, length: Int): Int {
            val end = start + length
            var result = 0
            var i = start
            while (i + 1 < end) {
                result += bytes.u16(i)
                i += 2
            }
            if (i < end) result += bytes.u8(i) shl 8
            return result
        }

        private fun checksum(sum: Int): Int {
            val folded = (sum and 65535) + (sum ushr 16)
            return ((folded and 65535) + (folded ushr 16)).inv() and 65535
        }
    }
}
