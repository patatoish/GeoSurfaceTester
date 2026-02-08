package com.geosurface.tester

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.geosurface.tester.ui.DiagnosticScreen
import com.geosurface.tester.ui.DiagnosticViewModel
import com.geosurface.tester.ui.theme.GeoSurfaceTesterTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: DiagnosticViewModel by viewModels()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted or denied - user can proceed with scan
        // The diagnostics will handle missing permissions gracefully
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request all necessary permissions
        requestPermissions()
        
        setContent {
            GeoSurfaceTesterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiagnosticScreen(viewModel = viewModel)
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_BASIC_PHONE_STATE)
        } else {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
