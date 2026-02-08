package com.geosurface.tester.diagnostics

import com.geosurface.tester.model.*
import java.util.*

class ConsistencyAnalyzer {
    
    fun analyze(
        network: NetworkInfo,
        location: LocationInfo,
        device: DeviceInfo,
        web: WebInfo
    ): ConsistencyReport {
        val mismatches = mutableListOf<Mismatch>()
        
        // Compare IP country vs System Timezone country
        val timezoneCountry = getCountryFromTimezone(device.systemTimezone)
        if (network.ipCountry != null && timezoneCountry != null) {
            if (!countriesMatch(network.ipCountry, timezoneCountry)) {
                mismatches.add(
                    Mismatch(
                        type = "IP vs Timezone",
                        details = "IP shows ${network.ipCountry} but timezone suggests $timezoneCountry",
                        severity = 3
                    )
                )
            }
        }
        
        // Compare IP country vs SIM country
        if (network.ipCountry != null && device.simCountryCode != null) {
            if (!countriesMatch(network.ipCountry, device.simCountryCode)) {
                mismatches.add(
                    Mismatch(
                        type = "IP vs SIM",
                        details = "IP shows ${network.ipCountry} but SIM is from ${device.simCountryCode}",
                        severity = 2
                    )
                )
            }
        }
        
        // Compare IP country vs System locale country
        if (network.ipCountry != null && device.systemCountry != null) {
            if (!countriesMatch(network.ipCountry, device.systemCountry)) {
                mismatches.add(
                    Mismatch(
                        type = "IP vs System Locale",
                        details = "IP shows ${network.ipCountry} but system locale is ${device.systemCountry}",
                        severity = 1
                    )
                )
            }
        }
        
        // Compare system timezone vs WebView timezone
        if (device.systemTimezone != null && web.jsTimezone != null) {
            if (device.systemTimezone != web.jsTimezone) {
                mismatches.add(
                    Mismatch(
                        type = "System vs Browser Timezone",
                        details = "System timezone is ${device.systemTimezone} but browser reports ${web.jsTimezone}",
                        severity = 2
                    )
                )
            }
        }
        
        // Check DNS country vs IP country
        if (network.dnsCountry != null && network.ipCountry != null) {
            if (!countriesMatch(network.dnsCountry, network.ipCountry)) {
                mismatches.add(
                    Mismatch(
                        type = "DNS vs IP Country",
                        details = "DNS resolver in ${network.dnsCountry} but IP shows ${network.ipCountry}",
                        severity = 1
                    )
                )
            }
        }
        
        // Check if roaming but IP country matches SIM country
        if (device.isRoaming && network.ipCountry != null && device.simCountryCode != null) {
            if (countriesMatch(network.ipCountry, device.simCountryCode)) {
                mismatches.add(
                    Mismatch(
                        type = "Roaming Inconsistency",
                        details = "Device shows roaming but IP country matches SIM country",
                        severity = 2
                    )
                )
            }
        }
        
        // Calculate score (0-100)
        // Start at 100, subtract points for each mismatch based on severity
        var score = 100
        mismatches.forEach { mismatch ->
            score -= when (mismatch.severity) {
                3 -> 25  // High severity
                2 -> 15  // Medium severity
                1 -> 10  // Low severity
                else -> 5
            }
        }
        score = score.coerceAtLeast(0)
        
        return ConsistencyReport(
            score = score,
            mismatches = mismatches
        )
    }
    
    private fun countriesMatch(country1: String, country2: String): Boolean {
        // Normalize and compare
        val c1 = country1.uppercase(Locale.ROOT).trim()
        val c2 = country2.uppercase(Locale.ROOT).trim()
        
        // Direct match
        if (c1 == c2) return true
        
        // Handle common variations
        val countryAliases = mapOf(
            "US" to listOf("USA", "UNITED STATES", "UNITED STATES OF AMERICA"),
            "UK" to listOf("GB", "GREAT BRITAIN", "UNITED KINGDOM"),
            "CN" to listOf("CHINA", "PEOPLE'S REPUBLIC OF CHINA"),
            "RU" to listOf("RUSSIA", "RUSSIAN FEDERATION"),
            "KR" to listOf("KOREA", "SOUTH KOREA", "REPUBLIC OF KOREA")
        )
        
        for ((code, aliases) in countryAliases) {
            val allVariants = listOf(code) + aliases
            if (allVariants.contains(c1) && allVariants.contains(c2)) {
                return true
            }
        }
        
        return false
    }
    
    private fun getCountryFromTimezone(timezone: String?): String? {
        if (timezone == null) return null
        
        // Map common timezones to countries
        val timezoneMap = mapOf(
            "America/New_York" to "US",
            "America/Chicago" to "US",
            "America/Los_Angeles" to "US",
            "America/Denver" to "US",
            "Europe/London" to "UK",
            "Europe/Paris" to "France",
            "Europe/Berlin" to "Germany",
            "Europe/Rome" to "Italy",
            "Europe/Madrid" to "Spain",
            "Asia/Tokyo" to "Japan",
            "Asia/Shanghai" to "China",
            "Asia/Hong_Kong" to "Hong Kong",
            "Asia/Singapore" to "Singapore",
            "Asia/Dubai" to "UAE",
            "Asia/Seoul" to "South Korea",
            "Australia/Sydney" to "Australia",
            "Australia/Melbourne" to "Australia",
            "Pacific/Auckland" to "New Zealand"
        )
        
        return timezoneMap[timezone] ?: run {
            // Try to extract country from timezone string (e.g., "Europe/Berlin" -> "Germany")
            val parts = timezone.split("/")
            if (parts.size >= 2) {
                parts[1].replace("_", " ")
            } else {
                null
            }
        }
    }
}
