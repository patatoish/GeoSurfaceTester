package com.geosurface.tester.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.geosurface.tester.model.LocationInfo
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationDiagnostics(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    suspend fun runDiagnostics(): LocationInfo = withContext(Dispatchers.IO) {
        val hasPermission = hasLocationPermission()
        
        if (!hasPermission) {
            return@withContext LocationInfo(
                locationPermissionGranted = false,
                playServicesAvailable = isPlayServicesAvailable()
            )
        }
        
        val gpsLocation = getGpsLocation()
        val networkLocation = getNetworkLocation()
        
        LocationInfo(
            gpsLatitude = gpsLocation?.latitude,
            gpsLongitude = gpsLocation?.longitude,
            gpsAccuracy = gpsLocation?.accuracy,
            networkLatitude = networkLocation?.latitude,
            networkLongitude = networkLocation?.longitude,
            networkAccuracy = networkLocation?.accuracy,
            locationPermissionGranted = true,
            playServicesAvailable = isPlayServicesAvailable(),
            playServicesVersion = getPlayServicesVersion()
        )
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
    
    private suspend fun getGpsLocation(): Location? {
        if (!hasLocationPermission()) return null
        
        return try {
            withTimeoutOrNull(10000) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
            }
        } catch (e: Exception) {
            Log.e("LocationDiagnostics", "Error getting GPS location", e)
            null
        }
    }
    
    private suspend fun getNetworkLocation(): Location? {
        if (!hasLocationPermission()) return null
        
        return try {
            withTimeoutOrNull(10000) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ).await()
            }
        } catch (e: Exception) {
            Log.e("LocationDiagnostics", "Error getting network location", e)
            null
        }
    }
    
    private fun isPlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }
    
    private fun getPlayServicesVersion(): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                "com.google.android.gms",
                0
            )
            packageInfo.versionName
        } catch (e: Exception) {
            Log.e("LocationDiagnostics", "Error getting Play Services version", e)
            null
        }
    }
}
