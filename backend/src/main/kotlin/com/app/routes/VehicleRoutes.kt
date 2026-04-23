package com.app.routes

import com.app.models.CreateVehicle
import com.app.models.Vehicle
import com.app.models.Vehicles
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ErrorResponse(val error: String)

fun Route.vehicleRoutes() {

    // GET /api/vehicles
    get("/api/vehicles") {
        try {
            val vehicles = transaction {
                Vehicles.selectAll().orderBy(Vehicles.name).map { row ->
                    Vehicle(
                        id = row[Vehicles.id].value,
                        name = row[Vehicles.name],
                        licensePlate = row[Vehicles.licensePlate],
                    )
                }
            }
            call.respond(HttpStatusCode.OK, vehicles)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
        }
    }

    // POST /api/vehicles
    post("/api/vehicles") {
        try {
            val body = call.receive<CreateVehicle>()
            if (body.name.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("name must not be blank"))
            }
            val created = transaction {
                val stmt = Vehicles.insert {
                    it[name] = body.name.trim()
                    it[licensePlate] = body.licensePlate.trim()
                }
                Vehicle(
                    id = stmt[Vehicles.id].value,
                    name = body.name.trim(),
                    licensePlate = body.licensePlate.trim(),
                )
            }
            call.respond(HttpStatusCode.Created, created)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
        }
    }
}
