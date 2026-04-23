```kotlin
// FILE: backend/settings.gradle.kts
rootProject.name = "app"
```
Sets the Gradle root project name to "app".

```kotlin
// FILE: backend/build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.serialization") version "1.9.25"
    application
}

group = "com.app"
version = "1.0.0"

application {
    mainClass.set("com.app.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-server-cors:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("ch.qos.logback:logback-classic:1.5.12")
}
```
Configures the Kotlin JVM project with all required Ktor, Exposed, SQLite, and Logback dependencies.

```properties
# FILE: backend/gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```
Points the Gradle wrapper to the Gradle 8.10 binary distribution.

```kotlin
// FILE: backend/src/main/kotlin/com/app/Application.kt
package com.app

import com.app.routes.tripRoutes
import com.app.routes.vehicleRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
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
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    tripRoutes()
    vehicleRoutes()
}
```
Entry point that initialises the database, configures Ktor with JSON serialisation and CORS, and registers all route handlers.

```kotlin
// FILE: backend/src/main/kotlin/com/app/DatabaseFactory.kt
package com.app

import com.app.models.Trips
import com.app.models.Vehicles
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    fun init() {
        File("data").mkdirs()
        Database.connect("jdbc:sqlite:data/app.db", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Vehicles, Trips)
            seedVehicles()
            seedTrips()
        }
    }

    private fun seedVehicles() {
        if (Vehicles.selectAll().count() == 0L) {
            listOf(
                Pair("Work Truck", "TRK-001"),
                Pair("Personal Car", "CAR-999"),
                Pair("Company Van", "VAN-042"),
            ).forEach { (name, plate) ->
                Vehicles.insert {
                    it[Vehicles.name] = name
                    it[Vehicles.licensePlate] = plate
                }
            }
        }
    }

    private fun seedTrips() {
        if (Trips.selectAll().count() == 0L) {
            val vehicleId = Vehicles.selectAll().first()[Vehicles.id].value
            listOf(
                Triple(10000.0, 10150.0, "Client site visit"),
                Triple(10150.0, 10320.0, "Supply run"),
                Triple(10320.0, 10500.0, "Airport drop-off"),
            ).forEach { (start, end, purpose) ->
                Trips.insert {
                    it[Trips.vehicleId] = vehicleId
                    it[Trips.date] = "2026-04-01"
                    it[Trips.startOdometer] = start
                    it[Trips.endOdometer] = end
                    it[Trips.miles] = end - start
                    it[Trips.purpose] = purpose
                    it[Trips.notes] = ""
                }
            }
        }
    }
}
```
Singleton that creates the SQLite database, runs schema migrations, and seeds initial vehicles and sample trips on first startup.

```kotlin
// FILE: backend/src/main/kotlin/com/app/models/Vehicle.kt
package com.app.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable

object Vehicles : IntIdTable("vehicles") {
    val name = varchar("name", 255)
    val licensePlate = varchar("license_plate", 50).default("")
}

@Serializable
data class Vehicle(
    val id: Int,
    val name: String,
    val licensePlate: String,
)

@Serializable
data class CreateVehicle(
    val name: String,
    val licensePlate: String = "",
)
```
Defines the `vehicles` Exposed table and its serialisable data classes for responses and request bodies.

```kotlin
// FILE: backend/src/main/kotlin/com/app/models/Trip.kt
package com.app.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable

object Trips : IntIdTable("trips") {
    val vehicleId = integer("vehicle_id").references(Vehicles.id)
    val date = varchar("date", 10)
    val startOdometer = double("start_odometer")
    val endOdometer = double("end_odometer")
    val miles = double("miles")
    val purpose = varchar("purpose", 500)
    val notes = varchar("notes", 1000).default("")
}

@Serializable
data class Trip(
    val id: Int,
    val vehicleId: Int,
    val vehicleName: String,
    val date: String,
    val startOdometer: Double,
    val endOdometer: Double,
    val miles: Double,
    val purpose: String,
    val notes: String,
)

@Serializable
data class CreateTrip(
    val vehicleId: Int,
    val date: String,
    val startOdometer: Double,
    val endOdometer: Double,
    val purpose: String,
    val notes: String = "",
)
```
Defines the `trips` Exposed table with a foreign key to `vehicles`, plus the serialisable `Trip` response and `CreateTrip` request body.

```kotlin
// FILE: backend/src/main/kotlin/com/app/routes/VehicleRoutes.kt
package com.app.routes

import com.app.models.CreateVehicle
import com.app.models.Vehicle
import com.app.models.Vehicles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ErrorResponse(val error: String)

fun Application.vehicleRoutes() {
    routing {

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
}
```
Implements `GET /api/vehicles` and `POST /api/vehicles` with full error handling and Exposed transactions.

```kotlin
// FILE: backend/src/main/kotlin/com/app/routes/TripRoutes.kt
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
```
Implements all trip endpoints (CRUD + summary), joining to the vehicles table to include vehicle names in responses and computing miles as `endOdometer - startOdometer`.
