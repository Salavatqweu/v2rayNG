package com.v2ray.ang.fakesni

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.v2ray.ang.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Runs the FakeSNI native binary inside the v2rayNG APK as root. */
class FakeSniService : Service() {
    companion object {
        const val ACTION_START = "com.v2ray.ang.fakesni.START"
        const val ACTION_STOP = "com.v2ray.ang.fakesni.STOP"
        const val EXTRA_CONNECT_HOST = "connect_host"
        const val EXTRA_CONNECT_PORT = "connect_port"
        const val EXTRA_FAKE_SNI = "fake_sni"

        const val LISTEN_HOST = "127.0.0.1"
        const val LISTEN_PORT = 40443
        private const val CHANNEL_ID = "fakesni"
        private const val NOTIFICATION_ID = 40443
        private const val BINARY_NAME = "sni-spoofing"
        private const val ASSET_ARM64 = "fakesni/sni-spoofing-arm64"
        private const val ASSET_ARM7 = "fakesni/sni-spoofing-arm7"

        @Volatile
        var running: Boolean = false
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val host = intent.getStringExtra(EXTRA_CONNECT_HOST).orEmpty()
                val port = intent.getIntExtra(EXTRA_CONNECT_PORT, 443)
                val fakeSni = intent.getStringExtra(EXTRA_FAKE_SNI).orEmpty()
                if (host.isNotBlank() && fakeSni.isNotBlank()) start(host, port, fakeSni)
            }
            ACTION_STOP -> stopProxy()
        }
        return START_NOT_STICKY
    }

    private fun start(connectHost: String, connectPort: Int, fakeSni: String) {
        if (running) return
        startForeground(NOTIFICATION_ID, notification("FakeSNI: $fakeSni → $connectHost:$connectPort"))
        job?.cancel()
        job = scope.launch {
            try {
                val binary = installBinary()
                val config = FakeSniPreferences.load(this@FakeSniService).copy(
                    enabled = true,
                    listenHost = LISTEN_HOST,
                    listenPort = LISTEN_PORT,
                    connectHost = connectHost,
                    connectPort = connectPort,
                    fakeSni = fakeSni,
                )
                val script = File(filesDir, "fakesni-launch.sh")
                script.writeText(buildScript(config, binary.absolutePath))
                script.setExecutable(true)

                process = Runtime.getRuntime().exec(arrayOf("su", "-c", "sh '${script.absolutePath}'"))
                running = true

                val stdout = launch { process?.inputStream?.bufferedReader()?.forEachLine { } }
                val stderr = launch { process?.errorStream?.bufferedReader()?.forEachLine { } }
                val exit = process?.waitFor() ?: -1
                stdout.join()
                stderr.join()
                if (running) running = false
                if (exit != 0) stopSelf()
            } catch (_: Exception) {
                running = false
                stopSelf()
            }
        }
    }

    private suspend fun installBinary(): File = withContext(Dispatchers.IO) {
        val asset = when {
            Build.SUPPORTED_ABIS.contains("arm64-v8a") -> ASSET_ARM64
            Build.SUPPORTED_ABIS.contains("armeabi-v7a") -> ASSET_ARM7
            else -> throw IllegalStateException("FakeSNI supports ARM64/ARMv7 only")
        }
        val file = File(filesDir, BINARY_NAME)
        assets.open(asset).use { input ->
            if (!file.exists() || file.length() == 0L) {
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        file.setExecutable(true)
        file
    }

    private fun buildScript(config: FakeSniConfig, binaryPath: String): String = """
#!/system/bin/sh
chmod +x '$binaryPath'
pkill -TERM -f '$BINARY_NAME' 2>/dev/null || true
sleep 0.2
${if (config.addIpRule) "ip rule add uidrange 0-0 lookup ${config.networkInterface} pref 1500 2>/dev/null || true" else ""}
exec ${config.binaryArgs(binaryPath)}
""".trimIndent()

    private fun stopProxy() {
        running = false
        job?.cancel()
        process?.destroy()
        process = null
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -TERM -f '$BINARY_NAME' 2>/dev/null || true")).waitFor()
        } catch (_: Exception) {
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "FakeSNI", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("FakeSNI")
            .setContentText(text)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        stopProxy()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
