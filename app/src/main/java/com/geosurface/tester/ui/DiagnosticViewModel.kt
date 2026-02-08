package com.geosurface.tester.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurface.tester.diagnostics.*
import com.geosurface.tester.model.DiagnosticResults
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiagnosticState {
    object Idle : DiagnosticState()
    object Loading : DiagnosticState()
    data class Success(val results: DiagnosticResults) : DiagnosticState()
    data class Error(val message: String) : DiagnosticState()
}

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _state = MutableStateFlow<DiagnosticState>(DiagnosticState.Idle)
    val state: StateFlow<DiagnosticState> = _state.asStateFlow()
    
    private val networkDiagnostics = NetworkDiagnostics(application)
    private val locationDiagnostics = LocationDiagnostics(application)
    private val deviceDiagnostics = DeviceDiagnostics(application)
    private val webViewDiagnostics = WebViewDiagnostics(application)
    private val consistencyAnalyzer = ConsistencyAnalyzer()
    
    fun runFullScan() {
        viewModelScope.launch {
            try {
                _state.value = DiagnosticState.Loading
                Log.d("DiagnosticViewModel", "Starting full scan...")
                
                // Run all diagnostics in parallel except WebView (needs Main thread)
                val networkDeferred = async { 
                    runCatching { networkDiagnostics.runDiagnostics() }
                        .getOrElse { 
                            Log.e("DiagnosticViewModel", "Network diagnostics failed", it)
                            throw it
                        }
                }
                val locationDeferred = async { 
                    runCatching { locationDiagnostics.runDiagnostics() }
                        .getOrElse { 
                            Log.e("DiagnosticViewModel", "Location diagnostics failed", it)
                            throw it
                        }
                }
                val deviceDeferred = async { 
                    runCatching { deviceDiagnostics.runDiagnostics() }
                        .getOrElse { 
                            Log.e("DiagnosticViewModel", "Device diagnostics failed", it)
                            throw it
                        }
                }
                
                // Wait for parallel tasks
                val network = networkDeferred.await()
                val location = locationDeferred.await()
                val device = deviceDeferred.await()
                
                Log.d("DiagnosticViewModel", "Parallel diagnostics complete, running WebView...")
                
                // WebView must run on Main thread
                val web = runCatching { webViewDiagnostics.runDiagnostics() }
                    .getOrElse { 
                        Log.e("DiagnosticViewModel", "WebView diagnostics failed", it)
                        throw it
                    }
                
                Log.d("DiagnosticViewModel", "All diagnostics complete, analyzing consistency...")
                
                // Analyze consistency
                val consistency = consistencyAnalyzer.analyze(network, location, device, web)
                
                val results = DiagnosticResults(
                    network = network,
                    location = location,
                    device = device,
                    web = web,
                    consistency = consistency
                )
                
                Log.d("DiagnosticViewModel", "Scan complete! Consistency score: ${consistency.score}")
                _state.value = DiagnosticState.Success(results)
                
            } catch (e: Exception) {
                Log.e("DiagnosticViewModel", "Error during scan", e)
                _state.value = DiagnosticState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
