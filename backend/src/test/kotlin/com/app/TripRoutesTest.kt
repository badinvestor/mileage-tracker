package com.app

import com.app.models.Trips
import com.app.models.Vehicles
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TripRoutesTest {

    private lateinit var dbFile: File
    private var vehicleId: Int = -1

    @BeforeTest
    fun setup() {
        dbFile = File.createTempFile("test_trips_", ".db")
        val db = Database.connect("jdbc:sqlite:${dbFile.absolutePath}", driver = "org.sqlite.JDBC")
        TransactionManager.defaultDatabase = db
        transaction {
            SchemaUtils.create(Vehicles, Trips)
            val stmt = Vehicles.insert {
                it[Vehicles.name] = "Test Truck"
                it[Vehicles.licensePlate] = "TST-1"
            }
            vehicleId = stmt[Vehicles.id].value
        }
    }

    @AfterTest
    fun teardown() {
        dbFile.delete()
    }

    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { module() }
        block()
    }

    /** POST a valid trip and return the response. */
    private suspend fun ApplicationTestBuilder.createTrip(
        start: Double = 1000.0,
        end: Double = 1100.0,
        purpose: String = "Test drive",
        date: String = "2026-04-15",
    ) = client.post("/api/trips") {
        contentType(ContentType.Application.Json)
        setBody(
            """{"vehicleId":$vehicleId,"date":"$date","startOdometer":$start,"endOdometer":$end,"purpose":"$purpose","notes":""}"""
        )
    }

    /** Extract the first `"id": <n>` from a JSON body. */
    private fun idFrom(body: String) =
        Regex(""""id"\s*:\s*(\d+)""").find(body)!!.groupValues[1]

    // ── GET /api/trips ───────────────────────────────────────────────────────

    @Test
    fun `GET trips returns 200 with empty list when no trips exist`() = withApp {
        val response = client.get("/api/trips")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().replace(Regex("\\s"), ""))
    }

    @Test
    fun `GET trips returns trips after creation`() = withApp {
        createTrip(purpose = "Airport run")
        val response = client.get("/api/trips")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Airport run")
    }

    @Test
    fun `GET trips filters by vehicleId`() = withApp {
        // create a second vehicle and a trip for it
        val otherBody = client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Other Car","licensePlate":"OTH-2"}""")
        }.bodyAsText()
        val otherId = idFrom(otherBody)

        createTrip(purpose = "My trip")
        client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":$otherId,"date":"2026-04-15","startOdometer":500.0,"endOdometer":600.0,"purpose":"Other trip","notes":""}""")
        }

        val body = client.get("/api/trips?vehicleId=$vehicleId").bodyAsText()
        assertContains(body, "My trip")
        assert(!body.contains("Other trip")) { "Filter did not exclude the other vehicle's trip" }
    }

    @Test
    fun `GET trips filters by date range`() = withApp {
        createTrip(date = "2026-03-01", purpose = "March trip")
        createTrip(date = "2026-04-15", purpose = "April trip")

        val body = client.get("/api/trips?from=2026-04-01&to=2026-04-30").bodyAsText()
        assertContains(body, "April trip")
        assert(!body.contains("March trip")) { "Date filter did not exclude out-of-range trip" }
    }

    // ── GET /api/trips/:id ───────────────────────────────────────────────────

    @Test
    fun `GET trip by id returns 200 with trip details`() = withApp {
        val id = idFrom(createTrip(purpose = "Fetch me").bodyAsText())
        val response = client.get("/api/trips/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Fetch me")
    }

    @Test
    fun `GET trip by id returns 404 for missing trip`() = withApp {
        assertEquals(HttpStatusCode.NotFound, client.get("/api/trips/99999").status)
    }

    @Test
    fun `GET trip by id returns 400 for non-integer id`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/trips/not-a-number").status)
    }

    // ── POST /api/trips ──────────────────────────────────────────────────────

    @Test
    fun `POST trips returns 201 with computed miles and vehicle name`() = withApp {
        val response = createTrip(start = 2000.0, end = 2250.0, purpose = "Long haul")
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertContains(body, "250.0")       // miles = end - start
        assertContains(body, "Test Truck")  // vehicleName joined from vehicles table
        assertContains(body, "Long haul")
    }

    @Test
    fun `POST trips returns 400 when endOdometer equals startOdometer`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, createTrip(start = 500.0, end = 500.0).status)
    }

    @Test
    fun `POST trips returns 400 when endOdometer is less than startOdometer`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, createTrip(start = 500.0, end = 100.0).status)
    }

    @Test
    fun `POST trips returns 400 when purpose is blank`() = withApp {
        val response = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":$vehicleId,"date":"2026-04-15","startOdometer":100.0,"endOdometer":200.0,"purpose":"  ","notes":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "purpose")
    }

    @Test
    fun `POST trips returns 404 when vehicleId does not exist`() = withApp {
        val response = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":99999,"date":"2026-04-15","startOdometer":100.0,"endOdometer":200.0,"purpose":"Ghost vehicle","notes":""}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── PUT /api/trips/:id ───────────────────────────────────────────────────

    @Test
    fun `PUT trips returns 200 with updated fields`() = withApp {
        val id = idFrom(createTrip(start = 300.0, end = 400.0, purpose = "Original").bodyAsText())
        val response = client.put("/api/trips/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":$vehicleId,"date":"2026-04-20","startOdometer":300.0,"endOdometer":500.0,"purpose":"Updated","notes":"changed"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertContains(body, "Updated")
        assertContains(body, "200.0") // new miles
    }

    @Test
    fun `PUT trips returns 404 for missing trip`() = withApp {
        val response = client.put("/api/trips/99999") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":$vehicleId,"date":"2026-04-20","startOdometer":100.0,"endOdometer":200.0,"purpose":"Ghost","notes":""}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT trips returns 404 when vehicleId does not exist`() = withApp {
        val id = idFrom(createTrip().bodyAsText())
        val response = client.put("/api/trips/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":99999,"date":"2026-04-20","startOdometer":100.0,"endOdometer":200.0,"purpose":"Bad vehicle","notes":""}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── DELETE /api/trips/:id ────────────────────────────────────────────────

    @Test
    fun `DELETE trips returns 204 and trip no longer appears in list`() = withApp {
        val id = idFrom(createTrip(purpose = "Delete me").bodyAsText())

        assertEquals(HttpStatusCode.NoContent, client.delete("/api/trips/$id").status)

        val list = client.get("/api/trips").bodyAsText()
        assert(!list.contains("Delete me")) { "Deleted trip still appears in list" }
    }

    @Test
    fun `DELETE trips returns 404 for missing trip`() = withApp {
        assertEquals(HttpStatusCode.NotFound, client.delete("/api/trips/99999").status)
    }

    // ── GET /api/summary ─────────────────────────────────────────────────────

    @Test
    fun `GET summary returns 200 with correct totalMiles for the month`() = withApp {
        createTrip(start = 1000.0, end = 1100.0, date = "2026-04-10") // 100 miles
        createTrip(start = 1100.0, end = 1350.0, date = "2026-04-20") // 250 miles

        val response = client.get("/api/summary?month=2026-04")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertContains(body, "350.0")      // totalMiles
        assertContains(body, "Test Truck")
        assertContains(body, "2026-04")
    }

    @Test
    fun `GET summary excludes trips outside the requested month`() = withApp {
        createTrip(start = 1000.0, end = 1200.0, date = "2026-03-15") // 200 miles — wrong month
        createTrip(start = 1200.0, end = 1300.0, date = "2026-04-01") // 100 miles — correct

        val body = client.get("/api/summary?month=2026-04").bodyAsText()
        assertContains(body, "\"totalMiles\"")
        // 100.0 should appear; 200.0 should not be the total
        assertContains(body, "100.0")
    }

    @Test
    fun `GET summary returns 400 when month param is missing`() = withApp {
        val response = client.get("/api/summary")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "month")
    }

    @Test
    fun `GET summary returns 400 for malformed month param`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/summary?month=April-2026").status)
    }

    @Test
    fun `GET summary returns zero totalMiles for a month with no trips`() = withApp {
        val response = client.get("/api/summary?month=2020-01")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "0.0")
    }
}
