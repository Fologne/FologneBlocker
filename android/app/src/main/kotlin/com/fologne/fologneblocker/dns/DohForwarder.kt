package com.fologne.fologneblocker.dns

import okhttp3.ConnectionPool
import okhttp3.Call
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

internal enum class DohProvider(
    val id: String,
    val host: String,
    vararg val addresses: String,
) {
    CLOUDFLARE("cloudflare", "cloudflare-dns.com", "1.1.1.1", "2606:4700:4700::1111", "1.0.0.1", "2606:4700:4700::1001"),
    GOOGLE("google", "dns.google", "8.8.8.8", "2001:4860:4860::8888", "8.8.4.4", "2001:4860:4860::8844"),
}

internal class DohForwarder(provider: DohProvider, protect: (Socket) -> Boolean) : AutoCloseable {
    private val lock = Any()
    private var closed = false
    private var pending: Call? = null
    private val mediaType = "application/dns-message".toMediaType()
    private val endpoint = "https://${provider.host}/dns-query"
    private val addresses = provider.addresses.map { InetAddress.getByName(it) }
    private val client = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (hostname != provider.host) throw UnknownHostException(hostname)
                return addresses
            }
        })
        .socketFactory(ProtectedSockets(protect))
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .connectionPool(ConnectionPool(2, 15, TimeUnit.SECONDS))
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun resolve(wire: ByteArray): ByteArray {
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/dns-message")
            .post(wire.toRequestBody(mediaType))
            .build()
        val call = synchronized(lock) {
            if (closed) throw IOException("Resolvedor encerrado")
            client.newCall(request).also { pending = it }
        }
        try {
            call.execute().use { response ->
                val body = response.body ?: throw IOException("Resposta DoH sem corpo")
                val type = body.contentType()
                if (!response.isSuccessful || type?.type != "application" || type.subtype != "dns-message") {
                    throw IOException("Resposta HTTP incompatível com DoH")
                }
                val source = body.source()
                if (source.request(65_536L)) throw IOException("Resposta DNS acima do limite")
                return source.readByteArray()
            }
        } finally {
            synchronized(lock) { pending = null }
        }
    }

    override fun close() {
        synchronized(lock) {
            closed = true
            pending?.cancel()
        }
        client.connectionPool.evictAll()
    }
}

private class ProtectedSockets(private val protect: (Socket) -> Boolean) : SocketFactory() {
    override fun createSocket(): Socket {
        val socket = Socket()
        try {
            if (!protect(socket)) throw IOException("Socket fora da VPN indisponível")
            return socket
        } catch (error: Exception) {
            socket.close()
            throw error
        }
    }

    override fun createSocket(host: String, port: Int): Socket = unsupported()
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = unsupported()
    override fun createSocket(host: InetAddress, port: Int): Socket = unsupported()
    override fun createSocket(host: InetAddress, port: Int, localHost: InetAddress, localPort: Int): Socket = unsupported()

    private fun unsupported(): Nothing = throw IOException("A conexão exige proteção antes de abrir o socket")
}
