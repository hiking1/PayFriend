package com.payfriend.services

import java.security.MessageDigest
import java.time.Instant

/**
 * Tamper-evident audit trail. Each event's hash covers the previous hash plus the
 * event contents, so editing or removing any past event breaks every later link.
 *
 * Scaffold limits: in-memory only. A production version would persist to an
 * append-only store and anchor the head hash externally (e.g. a Kafka topic or WORM storage).
 */
data class AuditEvent(
    val seq: Long,
    val timestamp: String,
    val actor: String,
    val action: String,
    val payload: String,
    val prevHash: String,
    val hash: String
)

class AuditService {

    private val events = mutableListOf<AuditEvent>()

    @Synchronized
    fun record(actor: String, action: String, payload: String): AuditEvent {
        val seq = events.size + 1L
        val timestamp = Instant.now().toString() // captured once and used for BOTH the hash and the stored event
        val prevHash = events.lastOrNull()?.hash ?: GENESIS_HASH
        val event = AuditEvent(
            seq = seq,
            timestamp = timestamp,
            actor = actor,
            action = action,
            payload = payload,
            prevHash = prevHash,
            hash = computeHash(seq, timestamp, actor, action, payload, prevHash)
        )
        events += event
        return event
    }

    @Synchronized
    fun verifyChain(): Boolean {
        var expectedPrev = GENESIS_HASH
        for (e in events) {
            if (e.prevHash != expectedPrev) return false
            if (computeHash(e.seq, e.timestamp, e.actor, e.action, e.payload, e.prevHash) != e.hash) return false
            expectedPrev = e.hash
        }
        return true
    }

    @Synchronized
    fun snapshot(): List<AuditEvent> = events.toList()

    companion object {
        val GENESIS_HASH: String = sha256("PAYFRIEND_GENESIS")

        private fun computeHash(
            seq: Long, timestamp: String, actor: String, action: String, payload: String, prevHash: String
        ): String {
            // Length-prefix each field so "a|b" + "c" can never collide with "a" + "b|c".
            val body = listOf(seq.toString(), timestamp, actor, action, payload, prevHash)
                .joinToString("") { "${it.length}:$it" }
            return sha256(body)
        }

        private fun sha256(input: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
