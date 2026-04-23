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
        Database.connect(
            url = "jdbc:sqlite:data/app.db",
            driver = "org.sqlite.JDBC",
            setupConnection = { it.createStatement().execute("PRAGMA foreign_keys = ON") }
        )
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
