package com.payfriend.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QrPaymentDataTest {

    @Test
    fun parsesFixedAmountQr() {
        val data = QrPaymentData.parse("PAYFRIEND|MERCH-001|PayFriend_Demo_Cafe|201912345A|12.50")
        assertNotNull(data)
        assertEquals("MERCH-001", data!!.merchantId)
        assertEquals("PayFriend Demo Cafe", data.merchantName)
        assertEquals("201912345A", data.merchantUen)
        assertEquals(1250L, data.amountCents)
    }

    @Test
    fun emptyAmountMeansCustomerEntersIt() {
        val data = QrPaymentData.parse("PAYFRIEND|MERCH-001|Cafe|201912345A|")
        assertNotNull(data)
        assertNull(data!!.amountCents)
    }

    @Test
    fun rejectsMalformedPayloads() {
        assertNull(QrPaymentData.parse("https://example.com"))
        assertNull(QrPaymentData.parse("PAYFRIEND|MERCH-001|Cafe|201912345A"))               // too few fields
        assertNull(QrPaymentData.parse("PAYFRIEND|MERCH-001|Cafe|201912345A|1|extra"))       // too many fields
        assertNull(QrPaymentData.parse("OTHER|MERCH-001|Cafe|201912345A|1.00"))              // wrong prefix
        assertNull(QrPaymentData.parse("PAYFRIEND|bad id!|Cafe|201912345A|1.00"))            // bad merchant id
        assertNull(QrPaymentData.parse("PAYFRIEND|MERCH-001||201912345A|1.00"))              // empty name
    }

    @Test
    fun rejectsInvalidAmounts() {
        for (bad in listOf("0", "-5", "abc", "1.234", "99999999")) {
            assertNull("amount '$bad' should be rejected", QrPaymentData.parse("PAYFRIEND|M1|Cafe|UEN1|$bad"))
        }
    }

    @Test
    fun moneyParsingAndFormatting() {
        assertEquals(1250L, Money.parseToCents("12.5"))
        assertEquals(100L, Money.parseToCents("1"))
        assertEquals(1L, Money.parseToCents("0.01"))
        assertNull(Money.parseToCents(""))
        assertEquals("S\$12.50", Money.format(1250))
        assertEquals("S\$0.05", Money.format(5))
        assertEquals("-S\$1.00", Money.format(-100))
    }
}
