package com.payfriend

import com.payfriend.repository.InMemoryTransactionRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    /** Runs the app with an in-memory repository and WITHOUT loading application.conf modules. */
    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = MapApplicationConfig() }
        application { appModule(InMemoryTransactionRepository()) }
        block()
    }

    private suspend fun ApplicationTestBuilder.login(): String {
        val response = client.post("/api/v1/auth/demo-login")
        assertEquals(HttpStatusCode.OK, response.status)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
    }

    private fun payJson(key: String, cents: Long) =
        """{"idempotencyKey":"$key","merchantId":"MERCH-001","merchantName":"Demo Cafe","amountCents":$cents}"""

    @Test
    fun healthReportsValidAuditChain() = withApp {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"auditChainValid\":true"))
    }

    @Test
    fun protectedRoutesRequireAToken() = withApp {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/wallets/me").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/transactions").status)
    }

    @Test
    fun loginThenReadWallet() = withApp {
        val token = login()
        val response = client.get("/api/v1/wallets/me") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"balanceCents\":100000"))
    }

    @Test
    fun paymentIsCreatedThenReplayedThenRejectedWhenOverBalance() = withApp {
        val token = login()

        suspend fun pay(key: String, cents: Long) = client.post("/api/v1/transactions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payJson(key, cents))
        }

        assertEquals(HttpStatusCode.Created, pay("test-key-0001", 1250).status)
        assertEquals(HttpStatusCode.OK, pay("test-key-0001", 1250).status) // idempotent replay
        assertEquals(HttpStatusCode.Conflict, pay("test-key-0001", 9999).status)
        assertEquals(HttpStatusCode.UnprocessableEntity, pay("test-key-0002", 999_999).status)
        assertEquals(HttpStatusCode.BadRequest, pay("test-key-0003", 0).status)

        val wallet = client.get("/api/v1/wallets/me") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertTrue(wallet.bodyAsText().contains("\"balanceCents\":98750"))
    }

    @Test
    fun malformedBodyIsABadRequestNotAServerError() = withApp {
        val token = login()
        val response = client.post("/api/v1/transactions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("{not json")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
