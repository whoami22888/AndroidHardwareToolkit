@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.android.hardwaretoolkit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.android.hardwaretoolkit.ble.BleAdvertisement
import com.android.hardwaretoolkit.ble.BleScanner
import com.android.hardwaretoolkit.core.*
import com.android.hardwaretoolkit.usb.UsbManagerBridge

class MainActivity : ComponentActivity() {
    private var refreshKey by mutableIntStateOf(0)

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshKey++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissionsIfNeeded()

        setContent {
            MaterialTheme {
                ToolkitApp(
                    requestBluetooth = ::requestBluetoothPermissionsIfNeeded,
                    refreshKey = refreshKey
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey++
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            buildList {
                if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                }
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        } else {
            buildList {
                if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        if (required.isNotEmpty()) {
            permissions.launch(required.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun ToolkitApp(
    requestBluetooth: () -> Unit,
    refreshKey: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val registry = remember { ProviderRegistry() }
    val native = remember { NativeHardwareDetector(context) }
    val usb = remember { UsbManagerBridge(context, registry) }
    var providerRevision by remember { mutableIntStateOf(0) }

    DisposableEffect(usb) {
        usb.start { providerRevision++ }
        onDispose { usb.stop() }
    }

    LaunchedEffect(refreshKey) {
        native.detect().forEach(registry::upsert)
        usb.refresh { providerRevision++ }
    }

    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Providers", "Sub-GHz", "LF RFID", "BLE")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Android Hardware Toolkit 0.9.0") }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {},
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> Dashboard(registry)
                1 -> Providers(registry)
                2 -> RadioPane("Sub-GHz", registry, Capability.SUB_GHZ_RX, Capability.SUB_GHZ_TX, providerRevision)
                3 -> RadioPane("125/134.2-kHz LF RFID", registry, Capability.LF_RFID_RX, Capability.LF_RFID_TX, providerRevision)
                else -> BlePane(requestBluetooth)
            }
        }
    }
}

@Composable
private fun Dashboard(registry: ProviderRegistry) = Column(
    Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(9.dp)
) {
    Text("v0.9 native + external hardware layer", style = MaterialTheme.typography.headlineSmall)
    Text("Phone-native hardware is detected through Android APIs; external hardware extends capabilities the handset does not physically expose.")
    Text("Native providers: ${registry.all().count { it.transport == Transport.PHONE_NATIVE }}")
    Text("External providers: ${registry.all().count { it.transport != Transport.PHONE_NATIVE }}")
    Text("Unsupported Sub-GHz and LF RFID hardware is never simulated. A compatible physical adapter is required when the handset lacks that radio.")
}

@Composable
private fun Providers(registry: ProviderRegistry) = LazyColumn(
    Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    item { Text("Detected providers", style = MaterialTheme.typography.headlineSmall) }

    val providers = registry.all()
    if (providers.isEmpty()) {
        item { Text("No hardware providers detected.") }
    } else {
        items(providers, key = { it.id }) { provider ->
            Text(
                "${provider.name} | ${provider.transport} | " +
                    "connected=${provider.connected} ready=${provider.ready}\\n${provider.detail}"
            )
        }
    }
}

@Composable
private fun RadioPane(
    name: String,
    registry: ProviderRegistry,
    rx: Capability,
    tx: Capability,
    revision: Int
) = Column(
    Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    key(revision) {
        val readReady = registry.find(rx).isNotEmpty()
        val writeReady = registry.find(tx).isNotEmpty()
        Text(name, style = MaterialTheme.typography.headlineSmall)
        Text("Reader/RX: ${if (readReady) "provider ready" else "no compatible provider"}")
        Text("Writer/TX: ${if (writeReady) "provider ready" else "no compatible provider"}")
        Text("No operation is reported as working unless a detected provider has a concrete driver.")
    }
}

@Composable
private fun BlePane(requestBluetooth: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scanner = remember { BleScanner(context) }
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var advertisements by remember { mutableStateOf<Map<String, BleAdvertisement>>(emptyMap()) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(scanner) {
        scanner.onAdvertisement = { advertisement ->
            mainHandler.post {
                advertisements = advertisements + (advertisement.address to advertisement)
            }
        }
        scanner.onError = { code ->
            mainHandler.post {
                scanning = false
                error = "BLE scan failed with error code $code"
            }
        }

        onDispose {
            scanner.stop()
            scanner.onAdvertisement = null
            scanner.onError = null
        }
    }

    Column(
        Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("BLE scanner", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    requestBluetooth()
                    error = null
                },
                enabled = !scanning
            ) {
                Text("Check permissions")
            }

            Button(
                onClick = {
                    if (scanning) {
                        scanner.stop()
                        scanning = false
                    } else {
                        scanner.start()
                            .onSuccess {
                                scanning = true
                                error = null
                            }
                            .onFailure { failure ->
                                scanning = false
                                error = failure.message ?: failure::class.simpleName.orEmpty()
                            }
                    }
                }
            ) {
                Text(if (scanning) "Stop scan" else "Start scan")
            }
        }

        error?.let { Text("Error: $it") }
        Text("Advertisements: ${advertisements.size}")

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(advertisements.values.toList(), key = { it.address }) { advertisement ->
                Text(
                    "${advertisement.name} | ${advertisement.address} | RSSI ${advertisement.rssi}\\n" +
                        "Services: ${advertisement.serviceUuids.joinToString()}\\n" +
                        "Manufacturer: ${advertisement.manufacturerDataHex.ifEmpty { "none" }}"
                )
            }
        }
    }
}
