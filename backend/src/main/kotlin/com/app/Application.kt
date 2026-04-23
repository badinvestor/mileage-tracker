package com.app

import com.app.routes.authRoutes
import com.app.routes.tripRoutes
import com.app.routes.vehicleRoutes
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    DatabaseFactory.init()
    embeddedServer(Netty, port = 3001, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
    install(CORS) {
        allowHost("localhost:5173")
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "mileage-tracker"
            verifier(
                JWT.require(Algorithm.HMAC256(AuthConfig.jwtSecret))
                    .withIssuer("mileage-tracker")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("username").asString() == AuthConfig.adminUsername) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token missing or invalid"))
            }
        }
    }
    routing {
        authRoutes()
        authenticate("auth-jwt") {
            tripRoutes()
            vehicleRoutes()
        }
    }
}
