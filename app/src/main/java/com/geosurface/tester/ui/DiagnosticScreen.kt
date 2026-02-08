package com.geosurface.tester.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geosurface.tester.model.*
import com.geosurface.tester.ui.components.CardStatus
import com.geosurface.tester.ui.components.DataRow
import com.geosurface.tester.ui.components.ResultCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(viewModel: DiagnosticViewModel) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GeoSurface Tester") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scan Button
            Button(
                onClick = { viewModel.runFullScan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = state !is DiagnosticState.Loading
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state is DiagnosticState.Loading) "Scanning..." else "Run Full Scan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Content based on state
            when (val currentState = state) {
                is DiagnosticState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap 'Run Full Scan' to begin diagnostics",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                is DiagnosticState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Running diagnostics...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                is DiagnosticState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error: ${currentState.message}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Red
                            )
                        }
                    }
                }
                
                is DiagnosticState.Success -> {
                    DiagnosticResults(results = currentState.results)
                }
            }
        }
    }
}

@Composable
fun DiagnosticResults(results: DiagnosticResults) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Consistency Score Card
        ConsistencyScoreCard(consistency = results.consistency)
        
        // Mismatches Card
        if (results.consistency.mismatches.isNotEmpty()) {
            MismatchesCard(mismatches = results.consistency.mismatches)
        }
        
        // Network Info Card
        NetworkInfoCard(network = results.network)
        
        // Location Info Card
        LocationInfoCard(location = results.location)
        
        // Device Info Card
        DeviceInfoCard(device = results.device)
        
        // WebView Info Card
        WebInfoCard(web = results.web)
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ConsistencyScoreCard(consistency: ConsistencyReport) {
    val status = when {
        consistency.score >= 80 -> CardStatus.Success
        consistency.score >= 50 -> CardStatus.Warning
        else -> CardStatus.Error
    }
    
    ResultCard(title = "Location Consistency Score", status = status) {
        Text(
            text = "${consistency.score}/100",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                CardStatus.Success -> Color(0xFF4CAF50)
                CardStatus.Warning -> Color(0xFFFFA726)
                CardStatus.Error -> Color(0xFFF44336)
                else -> Color.Gray
            }
        )
        Text(
            text = when {
                consistency.score >= 80 -> "Excellent - All signals consistent"
                consistency.score >= 50 -> "Moderate - Some mismatches detected"
                else -> "Poor - Major contradictions found"
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MismatchesCard(mismatches: List<Mismatch>) {
    ResultCard(title = "⚠️ Detected Mismatches", status = CardStatus.Error) {
        mismatches.forEach { mismatch ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF44336).copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = mismatch.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = mismatch.details,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkInfoCard(network: NetworkInfo) {
    val status = if (network.publicIpv4 != null) CardStatus.Success else CardStatus.Warning
    
    ResultCard(title = "🌐 Network Information", status = status) {
        DataRow("Public IPv4", network.publicIpv4)
        DataRow("Public IPv6", network.publicIpv6)
        DataRow("IP Country", network.ipCountry)
        DataRow("ISP", network.isp)
        DataRow("ASN", network.asn)
        DataRow("DNS Resolver", network.dnsResolver)
        DataRow("DNS Country", network.dnsCountry)
        DataRow("IPv4 Available", if (network.hasIpv4) "Yes" else "No")
        DataRow("IPv6 Available", if (network.hasIpv6) "Yes" else "No")
    }
}

@Composable
fun LocationInfoCard(location: LocationInfo) {
    val status = when {
        !location.locationPermissionGranted -> CardStatus.Warning
        location.gpsLatitude != null -> CardStatus.Success
        else -> CardStatus.Info
    }
    
    ResultCard(title = "📍 Location Services", status = status) {
        if (!location.locationPermissionGranted) {
            Text(
                text = "⚠️ Location permission not granted",
                color = Color(0xFFFFA726),
                fontWeight = FontWeight.Bold
            )
        } else {
            DataRow("GPS Latitude", location.gpsLatitude?.toString())
            DataRow("GPS Longitude", location.gpsLongitude?.toString())
            DataRow("GPS Accuracy", location.gpsAccuracy?.let { "${it}m" })
            DataRow("Network Latitude", location.networkLatitude?.toString())
            DataRow("Network Longitude", location.networkLongitude?.toString())
            DataRow("Network Accuracy", location.networkAccuracy?.let { "${it}m" })
        }
        DataRow("Play Services", if (location.playServicesAvailable) "Available" else "Unavailable")
        DataRow("Play Services Version", location.playServicesVersion)
    }
}

@Composable
fun DeviceInfoCard(device: DeviceInfo) {
    ResultCard(title = "📱 Device Information", status = CardStatus.Info) {
        DataRow("SIM Country Code", device.simCountryCode)
        DataRow("Carrier Name", device.simCarrierName)
        DataRow("Roaming", if (device.isRoaming) "Yes" else "No")
        DataRow("Wi-Fi SSID", device.wifiSsid)
        DataRow("Wi-Fi BSSID", device.wifiBssid)
        DataRow("System Timezone", device.systemTimezone)
        DataRow("UTC Offset", device.utcOffset)
        DataRow("System Locale", device.systemLocale)
        DataRow("System Country", device.systemCountry)
        DataRow("System Language", device.systemLanguage)
        DataRow("Keyboard Language", device.keyboardLanguage)
    }
}

@Composable
fun WebInfoCard(web: WebInfo) {
    val status = if (web.jsTimezone != null) CardStatus.Success else CardStatus.Warning
    
    ResultCard(title = "🌐 WebView Fingerprint", status = status) {
        DataRow("JS Timezone", web.jsTimezone)
        DataRow("Intl Locale", web.intlLocale)
        DataRow("User-Agent", web.userAgent?.take(50)?.plus("..."))
        DataRow("Accept-Language", web.acceptLanguage)
        DataRow("Canvas Fingerprint", web.canvasFingerprint)
        DataRow("WebGL Renderer", web.webGlRenderer)
        DataRow("WebGL Vendor", web.webGlVendor)
    }
}
