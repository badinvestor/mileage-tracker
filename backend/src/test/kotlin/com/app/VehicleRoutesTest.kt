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

    @BeforeTest
    fun setup() {
        dbFile = File.createTempFile("test_vehicles_", ".db")
        val db = Database.connect("jdbc:sqlite:${dbFile.absolutePath}", driver = "org.sqlite.JDBC")
        TransactionManager.defaultDatabase = db
        transaction {
            SchemaUtils.create(Vehicles, Trips)
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

    // ── GET /api/vehicles ────────────────────────────────────────────────────

    @Test
    fun `GET vehicles returns 200 with empty list when no vehicles exist`() = withApp {
        val response = client.get("/api/vehicles")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().replace(Regex("\\s"), ""))
    }

    @Test
    fun `GET vehicles returns all vehicles sorted by name`() = withApp {
        // create two vehicles out of alphabetical order
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
        val alphaIdx = body.indexOf("Alpha Truck")
        val zebraIdx = body.indexOf("Zebra Van")
        assert(alphaIdx in 0..zebraIdx) { "Vehicles not sorted: alphaIdx=$alphaIdx zebraIdx=$zebraIdx" }
    }

    // ── POST /api/vehicles ───────────────────────────────────────────────────

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
        assertContains(body, "\"id\"")
    }

    @Test
    fun `POST vehicles returns 201 when licensePlate is empty`() = withApp {
        val response = client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Bare Bike","licensePlate":""}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertContains(response.bodyAsText(), "Bare Bike")
    }

    @Test
    fun `POST vehicles returns 400 when name is blank`() = withApp {
        val response = client.post("/api/vehicles") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"   ","licensePlate":"X-999"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "blank")
    }

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
