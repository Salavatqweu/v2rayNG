package com.v2ray.ang.fakesni

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class FakeSniSettingsActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val initial = remember { FakeSniPreferences.load(this@FakeSniSettingsActivity) }
            var enabled by remember { mutableStateOf(initial.enabled) }
            var fakeSni by remember { mutableStateOf(initial.fakeSni) }
            var utls by remember { mutableStateOf(initial.utls) }
            var injector by remember { mutableStateOf(initial.injector) }
            var port by remember { mutableStateOf(initial.listenPort.toString()) }
            var fakeRepeat by remember { mutableStateOf(initial.fakeRepeat.toString()) }
            var fakeDelay by remember { mutableStateOf(initial.fakeDelay) }
            var ackTimeout by remember { mutableStateOf(initial.ackTimeout) }
            var fragment by remember { mutableStateOf(initial.enableFragment) }
            var fragmentDelay by remember { mutableStateOf(initial.fragmentDelay) }
            var sniChunk by remember { mutableStateOf(initial.sniChunk.toString()) }
            var interfaceName by remember { mutableStateOf(initial.networkInterface) }
            var addIpRule by remember { mutableStateOf(initial.addIpRule) }

            Scaffold(topBar = {
                TopAppBar(title = { Text("FakeSNI") }, navigationIcon = {
                    Button(onClick = { finish() }) { Text("Back") }
                })
            }) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
                ) {
                    Text("Embedded SNI spoofing. Root access is required.")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text(if (enabled) "Enabled" else "Disabled")
                    OutlinedTextField(fakeSni, { fakeSni = it }, label = { Text("Fake SNI hostname") })
                    OutlinedTextField(utls, { utls = it }, label = { Text("uTLS fingerprint") })
                    OutlinedTextField(injector, { injector = it }, label = { Text("Injector (passive/active)") })
                    OutlinedTextField(port, { port = it }, label = { Text("Local port") })
                    OutlinedTextField(fakeRepeat, { fakeRepeat = it }, label = { Text("Fake repeat") })
                    OutlinedTextField(fakeDelay, { fakeDelay = it }, label = { Text("Fake delay") })
                    OutlinedTextField(ackTimeout, { ackTimeout = it }, label = { Text("ACK timeout") })
                    Switch(checked = fragment, onCheckedChange = { fragment = it })
                    Text("TLS ClientHello fragmentation")
                    OutlinedTextField(fragmentDelay, { fragmentDelay = it }, label = { Text("Fragment delay") })
                    OutlinedTextField(sniChunk, { sniChunk = it }, label = { Text("SNI chunk") })
                    OutlinedTextField(interfaceName, { interfaceName = it }, label = { Text("Fallback network interface") })
                    Switch(checked = addIpRule, onCheckedChange = { addIpRule = it })
                    Text("Add root routing rule")

                    Button(onClick = {
                        val config = initial.copy(
                            enabled = enabled,
                            listenPort = port.toIntOrNull()?.coerceIn(1024, 65535) ?: 40443,
                            fakeSni = fakeSni.trim(),
                            utls = utls.trim(),
                            injector = injector.trim(),
                            fakeRepeat = fakeRepeat.toIntOrNull()?.coerceIn(1, 20) ?: 1,
                            fakeDelay = fakeDelay.trim(),
                            ackTimeout = ackTimeout.trim(),
                            enableFragment = fragment,
                            fragmentDelay = fragmentDelay.trim(),
                            sniChunk = sniChunk.toIntOrNull()?.coerceIn(1, 64) ?: 3,
                            networkInterface = interfaceName.trim(),
                            addIpRule = addIpRule,
                        )
                        FakeSniPreferences.save(config)
                        if (!enabled) FakeSniManager.stop(this@FakeSniSettingsActivity)
                        finish()
                    }) { Text("Save") }
                }
            }
        }
    }
}
