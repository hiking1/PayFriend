package com.payfriend.plugins

import com.payfriend.models.ErrorResponse
import com.payfriend.models.HealthResponse
import com.payfriend.models.LoginResponse
import com.payfriend.routes.transactionRoutes
import com.payfriend.services.AuditService
import com.payfriend.services.LedgerService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.SerializationException

const val DEMO_USER_ID = "demo-customer-001"

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Malformed request body"))
        }
        exception<ContentConvertException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Malformed request body"))
        }
        exception<SerializationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Malformed request body"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
        }
    }
}

fun Application.configureRouting(ledger: LedgerService, audit: AuditService, jwt: JwtSettings) {
    routing {
        get("/health") {
            call.respond(HealthResponse("ok", audit.verifyChain()))
        }

        route("/api/v1") {
            // DEMO ONLY: stands in for real KYC / Singpass login. Remove before any real use.
            post("/auth/demo-login") {
                val wallet = LedgerService.walletOf(DEMO_USER_ID)
                ledger.ensureWallet(wallet)
                call.respond(LoginResponse(jwt.issueToken(DEMO_USER_ID), DEMO_USER_ID, wallet))
            }

            transactionRoutes(ledger, JWT_AUTH)
        }
    }
}
