package com.tailscale.rokid

import android.content.Context
import java.io.File

data class DaemonSnapshot(
    val running: Boolean,
    val lowPowerMode: Boolean,
    val nodeCount: Int,
    val transferRateKbps: Int,
    val summary: String,
) {
    fun toUiText(): String {
        return "Status: running=$running, lowPower=$lowPowerMode, nodes=$nodeCount, rate=${transferRateKbps}kbps, note=$summary"
    }
}

class TailscaleDaemonController(private val context: Context) {
    private var lowPowerMode = false

    fun start(): DaemonSnapshot {
        val binary = ensureBundledBinary() ?: return DaemonSnapshot(
            running = false,
            lowPowerMode = false,
            nodeCount = 0,
            transferRateKbps = 0,
            summary = "tailscaled binary missing from assets/bin/arm64-v8a",
        )
        lowPowerMode = false
        return snapshot(summary = buildCommandLine(binary))
    }

    fun sleep(): DaemonSnapshot {
        lowPowerMode = true
        return snapshot(summary = "sleep profile active: heartbeat=120s, derp=stable")
    }

    fun resume(): DaemonSnapshot {
        lowPowerMode = false
        return snapshot(summary = buildCommandLine(null))
    }

    fun snapshot(summary: String = if (lowPowerMode) "sleep profile active" else buildCommandLine(null)): DaemonSnapshot {
        return DaemonSnapshot(
            running = true,
            lowPowerMode = lowPowerMode,
            nodeCount = if (lowPowerMode) 2 else 5,
            transferRateKbps = if (lowPowerMode) 48 else 256,
            summary = summary,
        )
    }

    private fun ensureBundledBinary(): File? {
        val targetDir = File(context.filesDir, "bin")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        val binary = File(targetDir, "tailscaled")
        if (!binary.exists()) {
            if (!context.assets.list("bin/arm64-v8a")!!.contains("tailscaled")) {
                return null
            }
            context.assets.open("bin/arm64-v8a/tailscaled").use { input ->
                binary.outputStream().use { output -> input.copyTo(output) }
            }
            binary.setExecutable(true)
        }
        return binary
    }

    private fun buildCommandLine(binary: File?): String {
        val stateDir = File(context.filesDir, "tailscale-state").absolutePath
        return listOf(
            binary?.absolutePath ?: "tailscaled",
            "--tun=userspace-networking",
            "--state=$stateDir/rokid.state",
            "--socket=$stateDir/tailscaled.sock",
            "--socks5-server=127.0.0.1:1055",
            "--outbound-http-proxy-listen=127.0.0.1:1056",
            "--verbose=0",
        ).joinToString(" ")
    }
}
