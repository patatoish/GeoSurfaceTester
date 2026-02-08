package com.geosurface.tester.diagnostics

import android.content.Context
import android.util.Log
import com.geosurface.tester.model.NetworkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

class NetworkDiagnostics(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    suspend fun runDiagnostics(): NetworkInfo = withContext(Dispatchers.IO) {
        val ipv4 = checkIpv4Connectivity()
        val ipv6 = checkIpv6Connectivity()
        
        // Get public IP and geolocation data
        val ipData = getPublicIpAndLocation()
        
        Log.d("NetworkDiagnostics", "IPv4: $ipv4, IPv6: $ipv6, IP Data: $ipData")
        
        NetworkInfo(
            publicIpv4 = ipData["ipv4"],
            publicIpv6 = ipData["ipv6"],
            ipCountry = ipData["country"],
            isp = ipData["isp"],
            asn = ipData["asn"],
            dnsResolver = ipData["dnsResolver"],
            dnsCountry = ipData["dnsCountry"],
            hasIpv4 = ipv4,
            hasIpv6 = ipv6,
            webRtcLocalIps = emptyList(), // Will be populated by WebView
            webRtcPublicIp = null
        )
    }
    
    private fun checkIpv4Connectivity(): Boolean {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .any { it is Inet4Address && !it.isLoopbackAddress }
        } catch (e: Exception) {
            Log.e("NetworkDiagnostics", "Error checking IPv4", e)
            false
        }
    }
    
    private fun checkIpv6Connectivity(): Boolean {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .any { it is Inet6Address && !it.isLoopbackAddress }
        } catch (e: Exception) {
            Log.e("NetworkDiagnostics", "Error checking IPv6", e)
            false
        }
    }
    
    private fun getPublicIpAndLocation(): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        
        try {
            // Try ipapi.co for comprehensive IP data
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        result["ipv4"] = json.optString("ip", null)
                        result["country"] = json.optString("country_name", null)
                        result["isp"] = json.optString("org", null)
                        result["asn"] = json.optString("asn", null)
                        
                        // Try to get city/region for more detail
                        val city = json.optString("city", "")
                        val region = json.optString("region", "")
                        if (city.isNotEmpty() && region.isNotEmpty()) {
                            result["location"] = "$city, $region"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkDiagnostics", "Error getting IP data from ipapi.co", e)
            
            // Fallback to simpler API
            try {
                val request = Request.Builder()
                    .url("https://api.ipify.org?format=json")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            result["ipv4"] = json.optString("ip", null)
                        }
                    }
                }
            } catch (fallbackError: Exception) {
                Log.e("NetworkDiagnostics", "Error getting IP from ipify", fallbackError)
            }
        }
        
        // Try to get DNS resolver info
        try {
            val dnsRequest = Request.Builder()
                .url("https://1.1.1.1/cdn-cgi/trace")
                .build()
            
            client.newCall(dnsRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    body?.lines()?.forEach { line ->
                        when {
                            line.startsWith("loc=") -> {
                                result["dnsCountry"] = line.substringAfter("loc=")
                            }
                            line.startsWith("colo=") -> {
                                result["dnsResolver"] = "Cloudflare ${line.substringAfter("colo=")}"
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkDiagnostics", "Error getting DNS info", e)
        }
        
        return result
    }
}
