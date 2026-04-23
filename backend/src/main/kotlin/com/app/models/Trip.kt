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
