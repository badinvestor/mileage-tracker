package com.app

import at.favre.lib.crypto.bcrypt.BCrypt

object AuthConfig {
    val adminUsername: String = System.getenv("ADMIN_USERNAME") ?: "admin"
    val jwtSecret: String = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production"

    // Hash the configured password once at startup so every login verify() call
    // compares against it rather than re-hashing the raw value each time.
    private val rawPassword: String = System.getenv("ADMIN_PASSWORD") ?: "changeme"
    private val passwordHash: String = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray())

    fun verifyPassword(submitted: String): Boolean =
        BCrypt.verifyer().verify(submitted.toCharArray(), passwordHash).verified
}
