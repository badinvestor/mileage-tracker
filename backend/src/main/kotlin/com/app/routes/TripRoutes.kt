package com.app.routes

import com.app.models.CreateTrip
import com.app.models.Trip
import com.app.models.Trips
import com.app.models.Vehicles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class VehicleMiles(val vehicleId: Int, val vehicleName: String, val miles: Double)

@Serializable
data class SummaryResponse(val month: String, val totalMiles: Double, val byVehicle: List<VehicleMiles>)

fun Application.tripRoutes() {
    routing {
        route("/api/trips") {

            // GET /api/trips
            get {
                try {
                    val vehicleIdParam = call.request.queryParameters["vehicleId"]?.toIntOrNull()
                    val from = call.request.queryParameters["from"]
                    val to = call.request.queryParameters["to"]

                    val trips = transaction {
                        val join = Trips.join(Vehicles, JoinType.INNER, Trips.vehicleId, Vehicles.id)
                        var query = join.selectAll()
                        if (vehicleIdParam != null) {
                            query = query.andWhere { Trips.vehicleId eq vehicleIdParam }
                        }
                        if (!from.isNullOrBlank()) {
                            query = query.andWhere { Trips.date greaterEq from }
                        }
                        if (!to.isNullOrBlank()) {
                            query = query.andWhere { Trips.date lessEq to }
                        }
                        query.orderBy(Trips.date to SortOrder.DESC).map { row ->
                            Trip(
                                id = row[Trips.id].value,
                                vehicleId = row[Trips.vehicleId],
                                vehicleName = row[Vehicles.name],
                                date = row[Trips.date],
                                startOdometer = row[Trips.startOdometer],
                                endOdometer = row[Trips.endOdometer],
                                miles = row[Trips.miles],
                                purpose = row[Trips.purpose],
                                notes = row[Trips.notes],
                            )
                        }
                    }
                    call.respond(HttpStatusCode.OK, trips)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
                }
            }

            // GET /api/trips/:id
            get("{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                    val trip = transaction {
                        val join = Trips.join(Vehicles, JoinType.INNER, Trips.vehicleId, Vehicles.id)
                        join.selectAll().where { Trips.id eq id }.singleOrNull()?.let { row ->
                            Trip(
                                id = row[Trips.id].value,
                                vehicleId = row[Trips.vehicleId],
                                vehicleName = row[Vehicles.name],
                                date = row[Trips.date],
                                startOdometer = row[Trips.startOdometer],
                                endOdometer = row[Trips.endOdometer],
                                miles = row[Trips.miles],
                                purpose = row[Trips.purpose],
                                notes = row[Trips.notes],
                            )
                        }
                    }
                    if (trip == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Trip not found"))
                    } else {
                        call.respond(HttpStatusCode.OK, trip)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
                }
            }

            // POST /api/trips
            post {
                try {
                    val body = call.receive<CreateTrip>()
                    if (body.purpose.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("purpose must not be blank"))
                    }
                    if (body.startOdometer < 0) {
                        return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("startOdometer must be >= 0"))
                    }
                    if (body.endOdometer <= body.startOdometer) {
                        return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("endOdometer must be greater than startOdometer"))
                    }

                    val created = transaction {
                        val vehicle = Vehicles.selectAll().where { Vehicles.id eq body.vehicleId }.singleOrNull()
                            ?: return@transaction null
                        val computedMiles = body.endOdometer - body.startOdometer
                        val stmt = Trips.insert {
                            it[vehicleId] = body.vehicleId
                            it[date] = body.date
                            it[startOdometer] = body.startOdometer
                            it[endOdometer] = body.endOdometer
                            it[miles] = computedMiles
                            it[purpose] = body.purpose.trim()
                            it[notes] = body.notes.trim()
                        }
                        Trip(
                            id = stmt[Trips.id].value,
                            vehicleId = body.vehicleId,
                            vehicleName = vehicle[Vehicles.name],
                            date = body.date,
                            startOdometer = body.startOdometer,
                            endOdometer = body.endOdometer,
                            miles = computedMiles,
                            purpose = body.purpose.trim(),
                            notes = body.notes.trim(),
                        )
                    }

                    if (created == null) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Vehicle not found"))
                    } else {
                        // BUG: should be HttpStatusCode.Created (201), not OK (200)
                        call.respond(HttpStatusCode.OK, created)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
                }
            }

            // PUT /api/trips/:id
            put("{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                    val body = call.receive<CreateTrip>()
                    if (body.purpose.isBlank()) {
                        return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("purpose must not be blank"))
                    }
                    if (body.endOdometer <= body.startOdometer) {
                        return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("endOdometer must be greater than startOdometer"))
                    }

                    val updated = transaction {
                        val vehicle = Vehicles.selectAll().where { Vehicles.id eq body.vehicleId }.singleOrNull()
                            ?: return@transaction null to "vehicle"
                        val computedMiles = body.endOdometer - body.startOdometer
                        val count = Trips.update({ Trips.id eq id }) {
                            it[vehicleId] = body.vehicleId
                            it[date] = body.date
                            it[startOdometer] = body.startOdometer
                            it[endOdometer] = body.endOdometer
                            it[miles] = computedMiles
                            it[purpose] = body.purpose.trim()
                            it[notes] = body.notes.trim()
                        }
                        if (count == 0) return@transaction null to "trip"
                        Trip(
                            id = id,
                            vehicleId = body.vehicleId,
                            vehicleName = vehicle[Vehicles.name],
                            date = body.date,
                            startOdometer = body.startOdometer,
                            endOdometer = body.endOdometer,
                            miles = computedMiles,
                            purpose = body.purpose.trim(),
                            notes = body.notes.trim(),
                        ) to "ok"
                    }

                    when {
                        updated.first == null && updated.second == "vehicle" ->
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Vehicle not found"))
                        updated.first == null ->
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Trip not found"))
                        else ->
                            call.respond(HttpStatusCode.OK, updated.first!!)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
                }
            }

            // DELETE /api/trips/:id
            delete("{id}") {
                try {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                    val deleted = transaction {
                        Trips.deleteWhere { Trips.id eq id }
                    }
                    if (deleted == 0) {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Trip not found"))
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
                }
            }
        }

        // GET /api/summary?month=YYYY-MM
        get("/api/summary") {
            try {
                val month = call.request.queryParameters["month"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("month query parameter is required (YYYY-MM)"))
                if (!Regex("""\d{4}-\d{2}""").matches(month)) {
                    return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("month must be in YYYY-MM format"))
                }

                val result = transaction {
                    val join = Trips.join(Vehicles, JoinType.INNER, Trips.vehicleId, Vehicles.id)
                    val rows = join.selectAll()
                        .where { Trips.date like "$month%" }
                        .map { row ->
                            Triple(row[Trips.vehicleId], row[Vehicles.name], row[Trips.miles])
                        }
                    val byVehicle = rows.groupBy({ it.first to it.second }, { it.third })
                        .map { (key, milesList) ->
                            VehicleMiles(
                                vehicleId = key.first,
                                vehicleName = key.second,
                                miles = milesList.sum(),
                            )
                        }
                        .sortedByDescending { it.miles }
                    val totalMiles = byVehicle.sumOf { it.miles }
                    SummaryResponse(month = month, totalMiles = totalMiles, byVehicle = byVehicle)
                }
                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unexpected error"))
            }
        }
    }
}
