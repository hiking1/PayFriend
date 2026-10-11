package com.payfriend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.util.Date

const val JWT_AUTH = "auth-jwt"
private const val DEV_SECRET = "dev-only-secret-change-me-before-any-real-use"
private const val TOKEN_TTL_MILLIS = 60 * 60 * 1000L

class JwtSettings(val secret: String, val issuer: String, val audience: String) {
    private val algorithm: Algorithm get() = Algorithm.HMAC256(secret)

    fun issueToken(userId: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_TTL_MILLIS))
            .sign(algorithm)

    internal fun verifier() = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build()
}

/** Installs JWT authentication and returns the settings used to issue tokens. */
fun Application.configureSecurity(): JwtSettings {
    val cfg = environment.config
    val settings = JwtSettings(
        secret = cfg.propertyOrNull("payfriend.jwt.secret")?.getString() ?: DEV_SECRET,
        issuer = cfg.propertyOrNull("payfriend.jwt.issuer")?.getString() ?: "payfriend",
        audience = cfg.propertyOrNull("payfriend.jwt.audience")?.getString() ?: "payfriend-app"
    )
    if (settings.secret == DEV_SECRET) {
        environment.log.warn("Using the built-in DEMO JWT secret. Set JWT_SECRET before any real use.")
    }
    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = "payfriend"
            verifier(settings.verifier())
            validate { credential ->
                if (credential.payload.subject.isNullOrBlank()) null else JWTPrincipal(credential.payload)
            }
        }
    }
    return settings
}
