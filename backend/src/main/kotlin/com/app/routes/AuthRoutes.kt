package com.app.routes

import com.app.AuthConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

fun Route.authRoutes() {
    post("/api/auth/login") {
        val body = call.receive<LoginRequest>()
        if (body.username != AuthConfig.adminUsername || !AuthConfig.verifyPassword(body.password)) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            return@post
        }
        val token = JWT.create()
            .withIssuer("mileage-tracker")
            .withClaim("username", body.username)
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000)) // 24 hours
            .sign(Algorithm.HMAC256(AuthConfig.jwtSecret))
        call.respond(HttpStatusCode.OK, LoginResponse(token))
    }
}
