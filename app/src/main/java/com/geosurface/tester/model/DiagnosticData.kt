package com.geosurface.tester.model

data class NetworkInfo(
    val publicIpv4: String? = null,
    val publicIpv6: String? = null,
    val ipCountry: String? = null,
    val isp: String? = null,
    val asn: String? = null,
    val dnsResolver: String? = null,
    val dnsCountry: String? = null,
    val webRtcLocalIps: List<String> = emptyList(),
    val webRtcPublicIp: String? = null,
    val hasIpv4: Boolean = false,
    val hasIpv6: Boolean = false
)

data class LocationInfo(
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val networkLatitude: Double? = null,
    val networkLongitude: Double? = null,
    val networkAccuracy: Float? = null,
    val playServicesAvailable: Boolean = false,
    val playServicesVersion: String? = null,
    val locationPermissionGranted: Boolean = false
)

data class DeviceInfo(
    val simCountryCode: String? = null,
    val simCarrierName: String? = null,
    val isRoaming: Boolean = false,
    val wifiSsid: String? = null,
    val wifiBssid: String? = null,
    val systemTimezone: String? = null,
    val utcOffset: String? = null,
    val systemLocale: String? = null,
    val systemCountry: String? = null,
    val systemLanguage: String? = null,
    val keyboardLanguage: String? = null,
    val androidCountryProperty: String? = null
)

data class WebInfo(
    val jsTimezone: String? = null,
    val intlLocale: String? = null,
    val userAgent: String? = null,
    val acceptLanguage: String? = null,
    val canvasFingerprint: String? = null,
    val webGlRenderer: String? = null,
    val webGlVendor: String? = null
)

data class Mismatch(
    val type: String,
    val details: String,
    val severity: Int // 1-3: 1=low, 2=medium, 3=high
)

data class ConsistencyReport(
    val score: Int, // 0-100
    val mismatches: List<Mismatch> = emptyList()
)

data class DiagnosticResults(
    val network: NetworkInfo = NetworkInfo(),
    val location: LocationInfo = LocationInfo(),
    val device: DeviceInfo = DeviceInfo(),
    val web: WebInfo = WebInfo(),
    val consistency: ConsistencyReport = ConsistencyReport(score = 0),
    val timestamp: Long = System.currentTimeMillis()
)
