package com.payfriend.domain.model

import java.util.Locale

/**
 * Merchant QR payload: `PAYFRIEND|<merchantId>|<merchantName, "_" for spaces>|<UEN>|<amount or empty>`.
 *
 * The QR content is untrusted input. Parsing is strict, and the backend re-validates everything.
 * A real deployment would resolve the merchant server-side from the id instead of trusting the name.
 */
data class QrPaymentData(
    val merchantId: String,
    val merchantName: String,
    val merchantUen: String,
    /** Fixed amount from the QR, or null when the customer should type the amount. */
    val amountCents: Long?
) {
    companion object {
        const val PREFIX = "PAYFRIEND"
        private val ID_REGEX = Regex("^[A-Za-z0-9_-]{1,64}$")
        private val UEN_REGEX = Regex("^[A-Za-z0-9]{0,20}$")

        fun parse(raw: String): QrPaymentData? {
            val parts = raw.trim().split("|")
            if (parts.size != 5 || parts[0] != PREFIX) return null

            val merchantId = parts[1]
            val name = parts[2].replace('_', ' ').trim()
            val uen = parts[3]
            val amountText = parts[4]

            if (!ID_REGEX.matches(merchantId)) return null
            if (name.isEmpty() || name.length > 120) return null
            if (!UEN_REGEX.matches(uen)) return null

            val amountCents = if (amountText.isBlank()) {
                null
            } else {
                Money.parseToCents(amountText) ?: return null
            }
            return QrPaymentData(merchantId, name, uen, amountCents)
        }
    }
}

object Money {
    /** Client-side sanity cap (SGD 10,000.00). The backend enforces its own limit. */
    const val MAX_CENTS = 1_000_000L

    /** Parses "12.50" style input. Returns null for non-numeric, zero, negative, >2 decimals or too large. */
    fun parseToCents(text: String): Long? {
        val value = text.trim().toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        if (value.stripTrailingZeros().scale() > 2) return null
        val cents = runCatching { value.movePointRight(2).longValueExact() }.getOrNull() ?: return null
        return cents.takeIf { it in 1..MAX_CENTS }
    }

    fun format(cents: Long): String {
        val abs = if (cents < 0) -cents else cents
        val sign = if (cents < 0) "-" else ""
        return String.format(Locale.US, "%sS\$%d.%02d", sign, abs / 100, abs % 100)
    }
}
