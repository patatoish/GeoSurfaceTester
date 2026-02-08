package com.geosurface.tester.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.ContextCompat
import com.geosurface.tester.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class DeviceDiagnostics(private val context: Context) {
    
    suspend fun runDiagnostics(): DeviceInfo = withContext(Dispatchers.IO) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        
        DeviceInfo(
            simCountryCode = getSimCountryCode(telephonyManager),
            simCarrierName = getCarrierName(telephonyManager),
            isRoaming = isRoaming(telephonyManager),
            wifiSsid = getWifiSsid(wifiManager),
            wifiBssid = getWifiBssid(wifiManager),
            systemTimezone = getSystemTimezone(),
            utcOffset = getUtcOffset(),
            systemLocale = getSystemLocale(),
            systemCountry = getSystemCountry(),
            systemLanguage = getSystemLanguage(),
            keyboardLanguage = getKeyboardLanguage(),
            androidCountryProperty = getAndroidCountryProperty()
        )
    }
    
    private fun getSimCountryCode(telephonyManager: TelephonyManager?): String? {
        return try {
            if (hasPhonePermission()) {
                telephonyManager?.simCountryIso?.uppercase(Locale.ROOT)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("DeviceDiagnostics", "Error getting SIM country code", e)
            null
        }
    }
    
    private fun getCarrierName(telephonyManager: TelephonyManager?): String? {
        return try {
            telephonyManager?.networkOperatorName?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e("DeviceDiagnostics", "Error getting carrier name", e)
            null
        }
    }
    
    private fun isRoaming(telephonyManager: TelephonyManager?): Boolean {
        return try {
            telephonyManager?.isNetworkRoaming ?: false
        } catch (e: Exception) {
            Log.e("DeviceDiagnostics", "Error checking roaming status", e)
            false
        }
    }
    
    private fun getWifiSsid(wifiManager: WifiManager?): String? {
        return try {
            if (!hasLocationPermission()) {
                return "Permission required"
            }
            
            val wifiInfo = wifiManager?.connectionInfo
            wifiInfo?.ssid?.removeSurrounding("\"")?.takeIf { it != "<unknown ssid>" }
        } catch (e: Exception) {
            Log.e("DeviceDiagnostics", "Error getting Wi-Fi SSID", e)
            null
        }
    }
    
    private fun getWifiBssid(wifiManager: WifiManager?): String? {
        return try {
            if (!hasLocationPermission()) {
                return "Permission required"
            }
            
            val wifiInfo = wifiManager?.connectionInfo
            wifiInfo?.bssid
        } catch (e: Exception) {
            Log.e("DeviceDiagnostics", "Error getting Wi-Fi BSSID", e)
            null
        }
    }
    
    private fun getSystemTimezone(): String {
        return TimeZone.getDefault().id
    }
    
    private fun getUtcOffset(): String {
        val tz = TimeZone.getDefault()
        val offset = tz.rawOffset / 1000 / 60 / 60
        return "UTC${if (offset >= 0) "+" else ""}$offset"
    }
    
    private fun getSystemLocale(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].toString()
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.toString()
        }
    }
    
    private fun getSystemCountry(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].country
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.country
        }
    }
    
    private fun getSystemLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }
    
    private fun getKeyboardLanguage(): String? {
        return try {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
            // Note: Getting active keyboard language requires additional APIs
            // For now, return system language as approximation
            getSystemLanguage()
        } catch (e: Exception) {
            Log.e("DeviceDiagnostics", "Error getting keyboard language", e)
            null
        }
    }
    
    private fun getAndroidCountryProperty(): String? {
        return try {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            locale.country
        } catch (e: Exception) {
            Log.e("DeviceDiagnostics", "Error getting Android country property", e)
            null
        }
    }
    
    private fun hasPhonePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_BASIC_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
