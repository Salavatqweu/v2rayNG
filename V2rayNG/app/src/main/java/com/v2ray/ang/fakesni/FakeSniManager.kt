package com.v2ray.ang.fakesni

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.v2ray.ang.dto.entities.ProfileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/** Coordinates FakeSNI with a selected v2rayNG profile. */
object FakeSniManager {
    suspend fun startForProfile(context: Context, profile: ProfileItem): Boolean = withContext(Dispatchers.IO) {
        val prefs = FakeSniPreferences.load(context)
        if (!prefs.enabled) return@withContext true

        val connectHost = profile.server?.trim().orEmpty()
        val connectPort = profile.serverPort?.toIntOrNull() ?: 443
        if (connectHost.isBlank()) return@withContext false

        val fakeSni = prefs.fakeSni.ifBlank { profile.sni?.trim().orEmpty() }.ifBlank { "hcaptcha.com" }
        val intent = Intent(context, FakeSniService::class.java).apply {
            action = FakeSniService.ACTION_START
            putExtra(FakeSniService.EXTRA_CONNECT_HOST, connectHost)
            putExtra(FakeSniService.EXTRA_CONNECT_PORT, connectPort)
            putExtra(FakeSniService.EXTRA_FAKE_SNI, fakeSni)
        }
        ContextCompat.startForegroundService(context, intent)

        // The core must not race the local proxy. Give the foreground service a short
        // window to install the root binary and bind 127.0.0.1:40443.
        repeat(30) {
            if (isListening(FakeSniService.LISTEN_PORT)) return@withContext true
            delay(100)
        }
        false
    }

    fun stop(context: Context) {
        context.startService(Intent(context, FakeSniService::class.java).setAction(FakeSniService.ACTION_STOP))
    }

    fun rewriteOutboundAddress(address: String?, port: Int): Pair<String, Int> {
        return if (FakeSniPreferences.isEnabled()) {
            FakeSniService.LISTEN_HOST to FakeSniService.LISTEN_PORT
        } else {
            (address.orEmpty() to port)
        }
    }

    private fun isListening(port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(FakeSniService.LISTEN_HOST, port), 100)
            true
        }
    } catch (_: Exception) {
        false
    }
}
