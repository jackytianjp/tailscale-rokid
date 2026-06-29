package com.tailscale.rokid

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusView: TextView
    private lateinit var commandResultView: TextView
    private lateinit var deviceProfileView: TextView
    private lateinit var hudController: SpatialHudController
    private val voiceInterpreter = VoiceCommandInterpreter()

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (spoken.isBlank()) {
            commandResultView.text = "Voice: no actionable command recognized"
            return@registerForActivityResult
        }
        val action = voiceInterpreter.interpret(spoken)
        commandResultView.text = "Voice: $spoken -> ${action.description}"
        TailscaleForegroundService.handleVoiceAction(this, action)
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.statusView)
        commandResultView = findViewById(R.id.commandResultView)
        deviceProfileView = findViewById(R.id.deviceProfileView)
        hudController = SpatialHudController(this)

        val profile = RokidCapabilityProfile.fromDevice(Build.MODEL, Build.DEVICE)
        deviceProfileView.text = profile.describe()

        findViewById<Button>(R.id.startButton).setOnClickListener {
            startService(TailscaleForegroundService.intent(this, TailscaleForegroundService.ACTION_START))
            refreshStatus()
        }
        findViewById<Button>(R.id.suspendButton).setOnClickListener {
            startService(TailscaleForegroundService.intent(this, TailscaleForegroundService.ACTION_SLEEP))
            refreshStatus()
        }
        findViewById<Button>(R.id.overlayButton).setOnClickListener {
            ensureOverlayPermission()
            hudController.show(TailscaleForegroundService.snapshot())
        }
        findViewById<Button>(R.id.voiceButton).setOnClickListener {
            startVoiceCapture()
        }
        findViewById<Button>(R.id.permissionButton).setOnClickListener {
            requestCompliancePermissions()
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        statusView.text = TailscaleForegroundService.snapshot().toUiText()
    }

    private fun requestCompliancePermissions() {
        ensureOverlayPermission()
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        requestPermissions(
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            101,
        )
    }

    private fun ensureOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            startActivity(intent)
        }
    }

    private fun startVoiceCapture() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a Tailscale command")
        }
        speechLauncher.launch(intent)
    }
}
