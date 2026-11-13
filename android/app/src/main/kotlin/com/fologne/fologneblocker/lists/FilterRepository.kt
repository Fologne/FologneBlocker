package com.fologne.fologneblocker.lists

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.system.ErrnoException
import android.system.Os
import com.fologne.fologneblocker.dns.DnsStatistics
import com.fologne.fologneblocker.dns.RuleSnapshot
import com.fologne.fologneblocker.dns.normalizeDomain
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.Proxy
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.Locale

internal enum class ListSource(val title: String, val enabledByDefault: Boolean) {
    MULTI("HaGeZi Multi Normal", true),
    NSFW("HaGeZi NSFW", false);

    val url: String
        get() = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/wildcard/${name.lowercase(Locale.ROOT)}-onlydomains.txt"
}

internal class FilterRepository private constructor(context: Context) {
    private val db = object : SQLiteOpenHelper(context, "filters.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE rules (domain TEXT PRIMARY KEY, allow INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE attempts (source TEXT PRIMARY KEY, time INTEGER NOT NULL, error TEXT)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
    private val folder = context.filesDir
    private val lists = mutableMapOf<ListSource, LocalList>()
    private val refreshLock = Any()
    private val client = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .proxy(Proxy.NO_PROXY)
        .build()

    val statistics = DnsStatistics()

    @Volatile private var snapshot: RuleSnapshot? = null

    fun rules(): RuleSnapshot = snapshot ?: synchronized(this) {
        snapshot ?: loadRules()
    }

    @Synchronized
    fun state(): Map<String, Any> {
        rules()
        val manual = readManualRules()
        return mapOf(
            "rules" to manual.map { (domain, allow) -> mapOf("domain" to domain, "allow" to allow) },
            "sources" to ListSource.entries.map { source ->
                val saved = lists[source]
                db.readableDatabase.rawQuery(
                    "SELECT time, error FROM attempts WHERE source = ?", arrayOf(source.name),
                ).use { row ->
                    val attempted = row.moveToFirst()
                    mapOf(
                        "id" to source.name, "name" to source.title, "url" to source.url,
                        "enabled" to enabled(source), "lastSuccess" to saved?.time,
                        "count" to saved?.domains?.size, "hash" to saved?.hash,
                        "lastAttempt" to if (attempted) row.getLong(0) else null,
                        "error" to if (attempted && !row.isNull(1)) row.getString(1) else null,
                    )
                }
            },
        )
    }

    @Synchronized
    fun setEnabled(source: ListSource, enabled: Boolean) {
        rules()
        db.writableDatabase.replaceOrThrow("settings", null, ContentValues().apply {
            put("key", source.name)
            put("value", enabled)
        })
        publishRules()
    }

    @Synchronized
    fun saveRule(previous: String?, value: String, allow: Boolean) {
        val domain = requireNotNull(normalizeDomain(value))
        rules()
        val database = db.writableDatabase
        database.beginTransaction()
        try {
            if (previous != null && previous != domain) {
                database.delete("rules", "domain = ?", arrayOf(previous))
            }
            database.replaceOrThrow("rules", null, ContentValues().apply {
                put("domain", domain)
                put("allow", allow)
            })
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        publishRules()
    }

    @Synchronized
    fun deleteRule(domain: String) {
        rules()
        db.writableDatabase.delete("rules", "domain = ?", arrayOf(domain))
        publishRules()
    }

    fun refresh(source: ListSource? = null): Boolean = synchronized(refreshLock) {
        rules()
        val selected = if (source != null) listOf(source) else synchronized(this) {
            ListSource.entries.filter(::enabled)
        }
        var success = true
        for (entry in selected) {
            val time = System.currentTimeMillis()
            val error = try {
                val bytes = download(entry)
                val saved = decode(bytes, System.currentTimeMillis())
                synchronized(this) {
                    replaceFile(entry, bytes)
                    lists[entry] = saved
                    publishRules()
                }
                null
            } catch (_: IOException) {
                success = false
                "Falha ao baixar, validar ou salvar a lista. A última versão válida foi preservada."
            }
            synchronized(this) {
                db.writableDatabase.replaceOrThrow("attempts", null, ContentValues().apply {
                    put("source", entry.name)
                    put("time", time)
                    put("error", error)
                })
            }
        }
        success
    }

    private fun download(source: ListSource): ByteArray {
        val request = Request.Builder().url(source.url).build()
        client.newCall(request).execute().use { response ->
            if (response.code != 200) throw IOException("HTTP inválido")
            val body = response.body ?: throw IOException("Resposta vazia")
            if (body.contentLength() > MAX_LIST_BYTES) throw IOException("Lista muito grande")
            val input = body.source()
            if (input.request(MAX_LIST_BYTES.toLong() + 1)) throw IOException("Lista muito grande")
            return input.readByteArray()
        }
    }

    private fun loadRules(): RuleSnapshot {
        for (source in ListSource.entries) {
            try {
                val file = file(source)
                file.inputStream().use { input ->
                    if (input.channel.size() > MAX_LIST_BYTES) throw IOException("Lista muito grande")
                    lists[source] = decode(input.readBytes(), file.lastModified())
                }
            } catch (_: IOException) {
                // A fonte fica sem regras até obter uma lista válida.
            }
        }
        return publishRules()
    }

    private fun publishRules(): RuleSnapshot {
        val manual = readManualRules()
        return RuleSnapshot(
            manual.filterValues { it }.keys,
            manual.filterValues { !it }.keys,
            ListSource.entries.filter(::enabled).mapNotNull { lists[it]?.domains },
        ).also { snapshot = it }
    }

    private fun enabled(source: ListSource): Boolean =
        db.readableDatabase.rawQuery("SELECT value FROM settings WHERE key = ?", arrayOf(source.name)).use {
            if (it.moveToFirst()) it.getInt(0) != 0 else source.enabledByDefault
        }

    private fun readManualRules(): Map<String, Boolean> =
        db.readableDatabase.rawQuery("SELECT domain, allow FROM rules ORDER BY domain", null).use {
            buildMap {
                while (it.moveToNext()) put(it.getString(0), it.getInt(1) != 0)
            }
        }

    private fun file(source: ListSource) = File(folder, "hagezi-${source.name}.txt")

    // O rename no mesmo diretório mantém o arquivo anterior se a gravação falhar.
    private fun replaceFile(source: ListSource, bytes: ByteArray) {
        val target = file(source)
        val pending = File(folder, "${target.name}.new")
        try {
            FileOutputStream(pending).use {
                it.write(bytes)
                it.fd.sync()
            }
            Os.rename(pending.path, target.path)
        } catch (error: ErrnoException) {
            throw IOException(error)
        } finally {
            pending.delete()
        }
    }

    private fun decode(bytes: ByteArray, time: Long): LocalList {
        val text = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
        val domains = parseDomainList(text)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return LocalList(domains, time, hash)
    }

    private data class LocalList(val domains: Set<String>, val time: Long, val hash: String)

    companion object {
        @Volatile private var instance: FilterRepository? = null

        fun get(context: Context): FilterRepository = instance ?: synchronized(this) {
            instance ?: FilterRepository(context.applicationContext).also { instance = it }
        }
    }
}
