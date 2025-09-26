package com.bloomteq.fraud.domain.model.profile

import java.time.Instant

data class UserGeoProfile(
    val knownLocations: Set<LocationInfo> = emptySet(),
    val suspiciousLocations: Set<LocationInfo> = emptySet(),
    val lastLocation: LocationInfo? = null,
    val lastLocationUpdate: Instant? = null,
    val homeCountry: String? = null,
    val travelPattern: Map<String, Int> = emptyMap()
) {
    data class LocationInfo(
        val country: String,
        val city: String,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val firstSeen: Instant,
        val lastSeen: Instant,
        val transactionCount: Int = 1
    )
}