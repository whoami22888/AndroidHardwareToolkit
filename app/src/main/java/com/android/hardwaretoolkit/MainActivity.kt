package com.android.hardwaretoolkit

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.hardwaretoolkit.core.*
import com.android.hardwaretoolkit.usb.UsbManagerBridge
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity() {
    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ToolkitApp {
                    permissions.launch(arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ))
                }
            }
        }
    }
}

@Composable private fun ToolkitApp(requestBluetooth:()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val registry=remember{ProviderRegistry()}
    val usb=remember{UsbManagerBridge(context,registry)}
    var revision by remember{mutableIntStateOf(0)}
    DisposableEffect(Unit){usb.start{revision++};onDispose{usb.stop()}}
    var tab by remember{mutableIntStateOf(0)}
    val tabs=listOf("Dashboard","Providers","Sub-GHz","LF RFID","BLE")
    Scaffold(
        topBar={TopAppBar(title={Text("Android Hardware Toolkit 0.8.0")})},
        bottomBar={NavigationBar{tabs.forEachIndexed{i,s->
            NavigationBarItem(tab==i,{tab=i},icon={},label={Text(s)})}}}
    ){pad->Box(Modifier.padding(pad).fillMaxSize()){
        when(tab){
            0->Dashboard()
            1->Providers(registry,revision)
            2->RadioPane("Sub-GHz",registry,Capability.SUB_GHZ_RX,Capability.SUB_GHZ_TX)
            3->RadioPane("125/134.2-kHz LF RFID",registry,Capability.LF_RFID_RX,Capability.LF_RFID_TX)
            else->BlePane(requestBluetooth)
        }
    }}
}

@Composable private fun Dashboard()=Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
    Text("v0.8 physical adapter layer",style=MaterialTheme.typography.headlineSmall)
    Text("Only real providers can enable hardware operations.")
    Text("Unknown USB devices are shown as USB Host devices, never falsely identified as radios.")
    Text("Sub-GHz and LF RFID become operational only after a concrete documented adapter driver is installed.")
}

@Composable private fun Providers(registry:ProviderRegistry,revision:Int)=Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
    Text("Detected providers",style=MaterialTheme.typography.headlineSmall)
    val list=registry.all()
    if(list.isEmpty())Text("No USB providers detected.")
    list.forEach{Text("${it.name} | ${it.transport} | connected=${it.connected} ready=${it.ready}\n${it.detail}")}
}

@Composable private fun RadioPane(name:String,registry:ProviderRegistry,rx:Capability,tx:Capability)=Column(
    Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    val readReady=registry.find(rx).isNotEmpty()
    val writeReady=registry.find(tx).isNotEmpty()
    Text(name,style=MaterialTheme.typography.headlineSmall)
    Text("Reader/RX: ${if(readReady)"provider detected" else "no compatible provider"}")
    Text("Writer/TX: ${if(writeReady)"provider detected" else "no compatible provider"}")
    Text("A concrete driver is required before any RF I/O is enabled.")
}

@Composable private fun BlePane(requestBluetooth:()->Unit)=Column(
    Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Button(requestBluetooth){Text("Grant BLE permissions")}
    Text("BLE scanner implementation is present; scan results are not fabricated.")
}
