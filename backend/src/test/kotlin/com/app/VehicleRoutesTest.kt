package com.app

import com.app.models.Trips
import com.app.models.Vehicles
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class VehicleRoutesTest {

    private lateinit var dbFile: File

    // Runs before every test method.
    // Creates a fresh SQLite file and builds the schema from scratch so each
    // test starts with a completely empty database — no state leaks between tests.
    @BeforeTest
    fun setup() {
        dbFile = File.createTempFile("test_vehicles_", ".db")
        val db = Database.connect("jdbc:sqlite:${dbFile.absolutePath}", driver = "org.sqlite.JDBC")
        // Tell Exposed to use this connection for all transactions in this test.
        TransactionManager.defaultDatabase = db
        transaction {
            SchemaUtils.create(Vehicles, Trips)
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

    // ── GET /api/vehicles ────────────────────────────────────────────────────

    // Confirms the endpoint doesn't crash or return garbage on an empty database
    // and that the response body is a JSON empty array.
    @Test
    fun `GET vehicles returns 200 with empty list when no vehicles exist`() = withApp {
        val response = client.get("/api/vehicles")
        assertEquals(HttpStatusCode.OK, response.status)
        // Strip whitespace before comparing so pretty-printed `[ ]` matches `[]`.
        assertEquals("[]", response.bodyAsText().replace(Regex("\\s"), ""))
    }

    // Inserts two vehicles in reverse alphabetical order, then checks the list
    // comes back sorted A→Z. Catches a missing ORDER BY in the route handler.
    @Test
    fun `GET vehicles returns all vehicles sorted by name`() = withApp {
        client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Zebra Van","licensePlate":"Z-001"}""")
        }
        client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Alpha Truck","licensePlate":"A-001"}""")
        }

        val response = client.get("/api/vehicles")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        // "Alpha Truck" must appear earlier in the JSON string than "Zebra Van".
        val alphaIdx = body.indexOf("Alpha Truck")
        val zebraIdx = body.indexOf("Zebra Van")
        assert(alphaIdx in 0..zebraIdx) { "Vehicles not sorted: alphaIdx=$alphaIdx zebraIdx=$zebraIdx" }
    }

    // ── POST /api/vehicles ───────────────────────────────────────────────────

    // Verifies the happy path: the server inserts the vehicle, returns HTTP 201
    // (not 200), and the response body includes the generated id, name, and plate.
    @Test
    fun `POST vehicles returns 201 with created vehicle including id`() = withApp {
        val response = client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Work Truck","licensePlate":"TRK-001"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertContains(body, "Work Truck")
        assertContains(body, "TRK-001")
        // The response must include an "id" field so the frontend can reference it.
        assertContains(body, "\"id\"")
    }

    // licensePlate is optional in the API spec. Sending an empty string must
    // still succeed — this guards against an overly strict NOT NULL check.
    @Test
    fun `POST vehicles returns 201 when licensePlate is empty`() = withApp {
        val response = client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bare Bike","licensePlate":""}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertContains(response.bodyAsText(), "Bare Bike")
    }

    // A whitespace-only name must be rejected. Checks that the validation guard
    // fires before the INSERT and that the error body mentions "blank".
    @Test
    fun `POST vehicles returns 400 when name is blank`() = withApp {
        val response = client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"   ","licensePlate":"X-999"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "blank")
    }

    // Two-step persistence check: POST a vehicle, then GET the list and confirm
    // the vehicle actually appears. Catches a handler that returns 201 but
    // never commits the transaction.
    @Test
    fun `POST vehicles persists and GET vehicles reflects the new entry`() = withApp {
        client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Persist Van","licensePlate":"PV-1"}""")
        }
        val list = client.get("/api/vehicles").bodyAsText()
        assertContains(list, "Persist Van")
    }
}
