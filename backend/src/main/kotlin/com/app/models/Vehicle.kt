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
