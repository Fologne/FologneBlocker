package com.fologne.fologneblocker.dns

import com.fologne.fologneblocker.packet.put16
import com.fologne.fologneblocker.packet.u16
import com.fologne.fologneblocker.packet.u8
import java.io.ByteArrayOutputStream

internal class DnsQuery private constructor(
    private val id: Int,
    private val flags: Int,
    private val question: Question,
    val udpSize: Int,
    val wire: ByteArray,
) {
    fun answer(response: ByteArray, packetLimit: Int): ByteArray? {
        if (response.size < 12 || response.u16(0) != 0) return null
        val responseFlags = response.u16(2)
        if (responseFlags and 0xf840 != 0x8000 || response.u16(4) != 1) return null
        val echoed = question(response) ?: return null
        if (!question.sameAs(echoed)) return null
        if (responseFlags and 0x0200 != 0) return null
        if (scanRecords(response, echoed.end, response.u16(6), response.u16(8), response.u16(10)) == null) {
            return null
        }
        if (response.size > minOf(udpSize, packetLimit)) {
            return localResponse(responseFlags or 0x0200, includeOpt = false)
        }
        return response.copyOf().also { it.put16(0, id) }
    }

    fun failure(): ByteArray = localResponse(0x8082 or (flags and 0x0110), includeOpt = udpSize != 512 || wire.u16(10) != 0)

    private fun localResponse(responseFlags: Int, includeOpt: Boolean): ByteArray {
        val answer = ByteArray(12 + question.wire.size + if (includeOpt) 11 else 0)
        answer.put16(0, id)
        answer.put16(2, responseFlags and 0xffbf)
        answer.put16(4, 1)
        question.wire.copyInto(answer, 12)
        if (includeOpt) {
            answer.put16(10, 1)
            val opt = 12 + question.wire.size
            answer.put16(opt + 1, 41)
            answer.put16(opt + 3, minOf(udpSize, 1232))
        }
        return answer
    }

    companion object {
        fun parse(message: ByteArray): DnsQuery? {
            if (message.size < 12) return null
            val flags = message.u16(2)
            if (flags and 0xfecf != 0 || message.u16(4) != 1 ||
                message.u16(6) != 0 || message.u16(8) != 0 || message.u16(10) > 1
            ) return null
            val question = question(message) ?: return null
            // Nesta versão, a seção adicional da consulta aceita apenas EDNS(0).
            val opt = scanRecords(message, question.end, 0, 0, message.u16(10)) ?: return null
            if (message.u16(10) == 1 && opt.udpSize == null) return null
            if (opt.extendedCode != 0) return null
            val wire = message.copyOf()
            wire.put16(0, 0)
            return DnsQuery(message.u16(0), flags, question, opt.udpSize ?: 512, wire)
        }
    }
}

private data class Name(val wire: ByteArray, val end: Int)
private data class Question(val wire: ByteArray, val end: Int) {
    fun sameAs(other: Question): Boolean {
        if (wire.size != other.wire.size) return false
        val nameLength = wire.size - 4
        return wire.indices.all { index ->
            if (index < nameLength) fold(wire.u8(index)) == fold(other.wire.u8(index))
            else wire[index] == other.wire[index]
        }
    }

    private fun fold(value: Int) = if (value in 65..90) value + 32 else value
}

private fun question(message: ByteArray): Question? {
    val name = readName(message, 12) ?: return null
    if (name.end + 4 > message.size) return null
    return Question(name.wire + message.copyOfRange(name.end, name.end + 4), name.end + 4)
}

private fun readName(message: ByteArray, start: Int): Name? {
    var cursor = start
    var end = -1
    var pointerLimit = start
    val expanded = ByteArrayOutputStream()
    while (cursor < message.size) {
        val length = message.u8(cursor)
        if (length and 0xc0 == 0xc0) {
            if (cursor + 1 >= message.size) return null
            val pointer = message.u16(cursor) and 0x3fff
            if (pointer !in 12 until pointerLimit) return null
            if (end == -1) end = cursor + 2
            pointerLimit = pointer
            cursor = pointer
            continue
        }
        if (length > 63 || cursor + length >= message.size || expanded.size() + length + 1 > 255) return null
        expanded.write(message, cursor, length + 1)
        cursor += length + 1
        if (length == 0) return Name(expanded.toByteArray(), if (end == -1) cursor else end)
    }
    return null
}

private data class Records(val udpSize: Int?, val extendedCode: Int)

private fun scanRecords(message: ByteArray, start: Int, answers: Int, authorities: Int, extra: Int): Records? {
    var cursor = start
    var udpSize: Int? = null
    var extendedCode = 0
    val regular = answers + authorities
    val total = regular + extra
    if (total > (message.size - start) / 11) return null
    repeat(total) { index ->
        val name = readName(message, cursor) ?: return null
        val header = name.end
        if (header + 10 > message.size) return null
        val type = message.u16(header)
        val length = message.u16(header + 8)
        cursor = header + 10 + length
        if (cursor > message.size) return null
        if (type == 41) {
            if (index < regular || udpSize != null || name.wire.size != 1 || message.u8(header + 5) != 0) return null
            udpSize = maxOf(512, message.u16(header + 2))
            extendedCode = message.u8(header + 4)
            var option = header + 10
            while (option < cursor) {
                if (option + 4 > cursor) return null
                option += 4 + message.u16(option + 2)
                if (option > cursor) return null
            }
        } else if ((type == 1 && length != 4) || (type == 28 && length != 16)) {
            return null
        }
    }
    return if (cursor == message.size) Records(udpSize, extendedCode) else null
}
