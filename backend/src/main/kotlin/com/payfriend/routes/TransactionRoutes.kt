package com.payfriend.routes

import com.payfriend.models.ErrorResponse
import com.payfriend.models.PaymentRequest
import com.payfriend.models.WalletResponse
import com.payfriend.services.LedgerService
import com.payfriend.services.PayOutcome
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private fun ApplicationCall.walletId(): String {
    val subject = principal<JWTPrincipal>()?.payload?.subject ?: error("authenticated route without a principal")
    return LedgerService.walletOf(subject)
}

/** Mounted under /api/v1. Every route here requires a valid JWT. */
fun Route.transactionRoutes(ledger: LedgerService, jwtAuthName: String) {
    authenticate(jwtAuthName) {
        get("/wallets/me") {
            val wallet = call.walletId()
            call.respond(WalletResponse(wallet, ledger.balanceCents(wallet)))
        }

        route("/transactions") {
            get {
                call.respond(ledger.transactionsFor(call.walletId()))
            }

            get("/{id}") {
                val id = call.parameters["id"].orEmpty()
                val tx = ledger.transactionForPayer(id, call.walletId())
                if (tx == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Transaction not found"))
                } else {
                    call.respond(tx)
                }
            }

            post {
                val request = call.receive<PaymentRequest>()
                when (val outcome = ledger.pay(call.walletId(), request)) {
                    is PayOutcome.Created -> call.respond(HttpStatusCode.Created, outcome.transaction)
                    is PayOutcome.Replayed -> call.respond(HttpStatusCode.OK, outcome.transaction)
                    is PayOutcome.Rejected -> call.respond(
                        statusFor(outcome.code),
                        ErrorResponse(outcome.code, outcome.message)
                    )
                }
            }
        }
    }
}

private fun statusFor(code: String): HttpStatusCode = when (code) {
    "INSUFFICIENT_FUNDS" -> HttpStatusCode.UnprocessableEntity
    "IDEMPOTENCY_CONFLICT" -> HttpStatusCode.Conflict
    else -> HttpStatusCode.BadRequest
}
