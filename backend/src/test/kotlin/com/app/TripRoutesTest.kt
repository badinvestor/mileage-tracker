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
    // The id of the vehicle seeded in setup(), used as a valid foreign key in trip requests.
    private var vehicleId: Int = -1

    // Runs before every test method.
    // Creates a fresh SQLite temp file, builds the schema, and inserts one vehicle
    // so trip tests have a valid vehicleId to reference without repeating that boilerplate.
    @BeforeTest
    fun setup() {
        dbFile = File.createTempFile("test_trips_", ".db")
        val db = Database.connect("jdbc:sqlite:${dbFile.absolutePath}", driver = "org.sqlite.JDBC")
        // Tell Exposed to use this connection for all transactions in this test.
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

    // Runs after every test method.
    // Deletes the temp database file so it doesn't accumulate on disk.
    @AfterTest
    fun teardown() {
        dbFile.delete()
    }

    // Boots the full Ktor application in-process (no real network port).
    // The `client` inside the block sends requests directly to the test server.
    private fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { module() }
        block()
    }

    // Shared helper that POSTs a valid trip using the seeded vehicle.
    // Default values produce a 100-mile trip on 2026-04-15.
    // Tests override only the fields they care about, keeping test bodies short.
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

    // Parses the generated database id out of a JSON response body.
    // Used to capture the id from a POST response so it can be passed to
    // a subsequent GET, PUT, or DELETE in the same test.
    private fun idFrom(body: String) =
        Regex(""""id"\s*:\s*(\d+)""").find(body)!!.groupValues[1]

    // ── GET /api/trips ───────────────────────────────────────────────────────

    // Confirms the endpoint returns HTTP 200 and a JSON empty array on a fresh
    // database, guarding against crashes or wrong status codes on empty results.
    @Test
    fun `GET trips returns 200 with empty list when no trips exist`() = withApp {
        val response = client.get("/api/trips")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().replace(Regex("\\s"), ""))
    }

    // Inserts a trip via POST then checks it appears in the GET list.
    // Catches a handler that returns 201 but never commits to the database.
    @Test
    fun `GET trips returns trips after creation`() = withApp {
        createTrip(purpose = "Airport run")
        val response = client.get("/api/trips")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Airport run")
    }

    // Creates trips for two different vehicles and calls GET with a vehicleId
    // filter. Asserts only the matching vehicle's trip is returned and the
    // other is excluded. Catches a missing WHERE clause in the route handler.
    @Test
    fun `GET trips filters by vehicleId`() = withApp {
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

    // Creates one trip in March and one in April, then filters for April only.
    // Checks that the March trip is absent from the results.
    // Catches an off-by-one or missing date comparison in the route handler.
    @Test
    fun `GET trips filters by date range`() = withApp {
        createTrip(date = "2026-03-01", purpose = "March trip")
        createTrip(date = "2026-04-15", purpose = "April trip")

        val body = client.get("/api/trips?from=2026-04-01&to=2026-04-30").bodyAsText()
        assertContains(body, "April trip")
        assert(!body.contains("March trip")) { "Date filter did not exclude out-of-range trip" }
    }

    // ── GET /api/trips/:id ───────────────────────────────────────────────────

    // Creates a trip, extracts its id from the POST response, then fetches it
    // by id. Confirms the response body contains the original purpose field.
    @Test
    fun `GET trip by id returns 200 with trip details`() = withApp {
        val id = idFrom(createTrip(purpose = "Fetch me").bodyAsText())
        val response = client.get("/api/trips/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Fetch me")
    }

    // Requests an id (99999) that was never inserted.
    // Confirms the route returns 404 rather than 200 with an empty body or a crash.
    @Test
    fun `GET trip by id returns 404 for missing trip`() = withApp {
        assertEquals(HttpStatusCode.NotFound, client.get("/api/trips/99999").status)
    }

    // Sends a non-integer path segment. Confirms the toIntOrNull() guard in the
    // route fires and returns 400 instead of a 500 NumberFormatException.
    @Test
    fun `GET trip by id returns 400 for non-integer id`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/trips/not-a-number").status)
    }

    // ── POST /api/trips ──────────────────────────────────────────────────────

    // Full happy-path check: status is 201 (not 200), the computed miles field
    // equals end minus start (250), and the joined vehicleName is present.
    // Catches a wrong status code, a missing JOIN, or broken miles computation.
    @Test
    fun `POST trips returns 201 with computed miles and vehicle name`() = withApp {
        val response = createTrip(start = 2000.0, end = 2250.0, purpose = "Long haul")
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertContains(body, "250.0")       // miles = end - start
        assertContains(body, "Test Truck")  // vehicleName joined from vehicles table
        assertContains(body, "Long haul")
    }

    // Odometer readings that are identical (zero-distance trip) must be rejected.
    // The route should catch this before inserting a 0-mile row into the database.
    @Test
    fun `POST trips returns 400 when endOdometer equals startOdometer`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, createTrip(start = 500.0, end = 500.0).status)
    }

    // A negative distance (end < start) is physically impossible and must be
    // rejected. Catches a validation guard that only checks for equality.
    @Test
    fun `POST trips returns 400 when endOdometer is less than startOdometer`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, createTrip(start = 500.0, end = 100.0).status)
    }

    // A whitespace-only purpose must be rejected. Checks that the validation
    // fires before the INSERT and that the error body names the failing field.
    @Test
    fun `POST trips returns 400 when purpose is blank`() = withApp {
        val response = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":$vehicleId,"date":"2026-04-15","startOdometer":100.0,"endOdometer":200.0,"purpose":"  ","notes":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "purpose")
    }

    // Sending a vehicleId that does not exist in the vehicles table must return
    // 404, not 500. Confirms the route performs a vehicle existence check before
    // inserting the trip.
    @Test
    fun `POST trips returns 404 when vehicleId does not exist`() = withApp {
        val response = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":99999,"date":"2026-04-15","startOdometer":100.0,"endOdometer":200.0,"purpose":"Ghost vehicle","notes":""}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── PUT /api/trips/:id ───────────────────────────────────────────────────

    // Creates a trip (100 miles), then updates it to a new odometer range (200 miles)
    // and a new purpose. Confirms both the updated purpose and recalculated miles
    // appear in the response. Catches a handler that returns stale data.
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
        assertContains(body, "200.0") // new miles = 500 - 300
    }

    // Sends a PUT to an id that was never inserted.
    // Confirms the route returns 404 instead of silently updating zero rows.
    @Test
    fun `PUT trips returns 404 for missing trip`() = withApp {
        val response = client.put("/api/trips/99999") {
            contentType(ContentType.Application.Json)
            setBody("""{"vehicleId":$vehicleId,"date":"2026-04-20","startOdometer":100.0,"endOdometer":200.0,"purpose":"Ghost","notes":""}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // Attempts to reassign a trip to a vehicleId that does not exist.
    // Confirms the vehicle existence check inside the PUT handler fires and
    // returns 404 before the UPDATE is issued.
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

    // Creates a trip, deletes it, then fetches the full list.
    // Confirms the status is 204 (no body) and the trip no longer appears,
    // catching a soft-delete implementation that hides but doesn't remove rows.
    @Test
    fun `DELETE trips returns 204 and trip no longer appears in list`() = withApp {
        val id = idFrom(createTrip(purpose = "Delete me").bodyAsText())

        assertEquals(HttpStatusCode.NoContent, client.delete("/api/trips/$id").status)

        val list = client.get("/api/trips").bodyAsText()
        assert(!list.contains("Delete me")) { "Deleted trip still appears in list" }
    }

    // Deletes an id that was never inserted.
    // Confirms the deleteWhere count check fires and returns 404 rather than
    // silently returning 204 for a no-op delete.
    @Test
    fun `DELETE trips returns 404 for missing trip`() = withApp {
        assertEquals(HttpStatusCode.NotFound, client.delete("/api/trips/99999").status)
    }

    // ── GET /api/summary ─────────────────────────────────────────────────────

    // Inserts two trips in April (100 + 250 = 350 miles total) and requests the
    // summary for that month. Confirms totalMiles, the vehicle name (from the JOIN),
    // and the month string all appear in the response.
    @Test
    fun `GET summary returns 200 with correct totalMiles for the month`() = withApp {
        createTrip(start = 1000.0, end = 1100.0, date = "2026-04-10") // 100 miles
        createTrip(start = 1100.0, end = 1350.0, date = "2026-04-20") // 250 miles

        val response = client.get("/api/summary?month=2026-04")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertContains(body, "350.0")      // totalMiles = 100 + 250
        assertContains(body, "Test Truck") // vehicleName joined from vehicles table
        assertContains(body, "2026-04")
    }

    // Inserts one trip in March and one in April, then requests the April summary.
    // Confirms only the April trip's mileage (100) is counted, not March's (200).
    // Catches a missing WHERE clause that would sum all trips regardless of month.
    @Test
    fun `GET summary excludes trips outside the requested month`() = withApp {
        createTrip(start = 1000.0, end = 1200.0, date = "2026-03-15") // 200 miles — wrong month
        createTrip(start = 1200.0, end = 1300.0, date = "2026-04-01") // 100 miles — correct month

        val body = client.get("/api/summary?month=2026-04").bodyAsText()
        assertContains(body, "\"totalMiles\"")
        assertContains(body, "100.0") // only the April trip should be summed
    }

    // Omits the required `month` query parameter entirely.
    // Confirms the route returns 400 with an error body that mentions "month",
    // rather than crashing with a 500 NullPointerException.
    @Test
    fun `GET summary returns 400 when month param is missing`() = withApp {
        val response = client.get("/api/summary")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "month")
    }

    // Sends a month string in the wrong format ("April-2026" instead of "2026-04").
    // Confirms the regex validation in the handler fires and returns 400.
    @Test
    fun `GET summary returns 400 for malformed month param`() = withApp {
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/summary?month=April-2026").status)
    }

    // Requests a month that has no trips at all.
    // Confirms the handler returns 200 with totalMiles of 0.0 rather than
    // crashing or returning a 404.
    @Test
    fun `GET summary returns zero totalMiles for a month with no trips`() = withApp {
        val response = client.get("/api/summary?month=2020-01")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "0.0")
    }
}
