package com.payfriend

import com.payfriend.config.DatabaseConfig
import com.payfriend.plugins.configureRouting
import com.payfriend.plugins.configureSecurity
import com.payfriend.plugins.configureSerialization
import com.payfriend.plugins.configureStatusPages
import com.payfriend.repository.TransactionRepository
import com.payfriend.services.AuditService
import com.payfriend.services.LedgerService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging

fun main(args: Array<String>) = EngineMain.main(args)

/** Production entry point (referenced from application.conf). */
fun Application.module() {
    appModule(DatabaseConfig.createRepository(environment.config))
}

/** Wires the application around a given repository. Tests call this with an in-memory one. */
fun Application.appModule(repository: TransactionRepository) {
    val audit = AuditService()
    val ledger = LedgerService(repository, audit)

    install(CallLogging)
    configureSerialization()
    configureStatusPages()
    val jwt = configureSecurity()
    configureRouting(ledger, audit, jwt)
}
