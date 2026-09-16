package com.android.hardwaretoolkit

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.hardwaretoolkit.core.*
import com.android.hardwaretoolkit.usb.UsbManagerBridge

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ToolkitApp {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissions.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolkitApp(requestBluetooth: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val registry = remember { ProviderRegistry() }
    val native = remember { NativeHardwareDetector(context) }
    val usb = remember { UsbManagerBridge(context, registry) }
    var revision by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        native.detect().forEach(registry::upsert)
        usb.start { revision++ }
        onDispose { usb.stop() }
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
                2 -> RadioPane("Sub-GHz", registry, Capability.SUB_GHZ_RX, Capability.SUB_GHZ_TX, revision)
                3 -> RadioPane("125/134.2-kHz LF RFID", registry, Capability.LF_RFID_RX, Capability.LF_RFID_TX, revision)
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
    Text("Phone-native hardware is detected and used through Android APIs; external hardware extends capabilities the phone does not physically expose.")
    Text("Native providers: ${registry.all().count { it.transport == Transport.PHONE_NATIVE }}")
    Text("External providers: ${registry.all().count { it.transport != Transport.PHONE_NATIVE }}")
    Text("Sub-GHz and LF RFID are not falsely emulated. If the handset lacks the required RF hardware, a compatible physical adapter is required.")
}

@Composable
private fun Providers(registry: ProviderRegistry) = Column(
    Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(7.dp)
) {
    Text("Detected providers", style = MaterialTheme.typography.headlineSmall)
    val list = registry.all()
    if (list.isEmpty()) Text("No hardware providers detected.")
    list.forEach { provider ->
        Text("${provider.name} | ${provider.transport} | connected=${provider.connected} ready=${provider.ready}\n${provider.detail}")
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
        Text("Reader/RX: ${if (readReady) "provider detected" else "no compatible provider"}")
        Text("Writer/TX: ${if (writeReady) "provider detected" else "no compatible provider"}")
        Text("Native operation is used when the phone exposes suitable hardware. Otherwise a concrete documented external driver is required.")
    }
}

@Composable
private fun BlePane(requestBluetooth: () -> Unit) = Column(
    Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Button(onClick = requestBluetooth) { Text("Grant BLE permissions") }
    Text("BLE scanner uses the phone's native Bluetooth LE controller when available.")
}
