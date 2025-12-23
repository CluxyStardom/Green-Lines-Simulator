package com.example.greenlinessimulator

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false
    private lateinit var toggleButton: Button
    private lateinit var randomizeButton: Button
    private lateinit var greenCheckbox: CheckBox
    private lateinit var purpleCheckbox: CheckBox
    private lateinit var horizontalCheckbox: CheckBox
    private lateinit var lineCountSpinner: Spinner

    private companion object {
        private const val KEY_IS_SERVICE_RUNNING = "isServiceRunning"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleButton = findViewById(R.id.toggle_button)
        randomizeButton = findViewById(R.id.randomize_button)
        greenCheckbox = findViewById(R.id.green_checkbox)
        purpleCheckbox = findViewById(R.id.purple_checkbox)
        horizontalCheckbox = findViewById(R.id.horizontal_checkbox)
        lineCountSpinner = findViewById(R.id.line_count_spinner)

        if (savedInstanceState != null) {
            isServiceRunning = savedInstanceState.getBoolean(KEY_IS_SERVICE_RUNNING, false)
        }

        updateUiState(isServiceRunning)

        toggleButton.setOnClickListener {
            if (isServiceRunning) {
                stopService(Intent(this, GreenLineService::class.java))
                updateUiState(false)
            } else {
                startOverlayService()
            }
        }

        randomizeButton.setOnClickListener {
            if (isServiceRunning) {
                startOverlayService(randomize = true)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_SERVICE_RUNNING, isServiceRunning)
    }

    private fun startOverlayService(randomize: Boolean = false) {
        val selectedColors = ArrayList<String>()
        if (greenCheckbox.isChecked) {
            selectedColors.add("green")
        }
        if (purpleCheckbox.isChecked) {
            selectedColors.add("purple")
        }

        if (selectedColors.isEmpty()) {
            return // Don't start service if no color is selected
        }

        val lineCount = lineCountSpinner.selectedItemPosition + 1
        val orientation = if (horizontalCheckbox.isChecked) "horizontal" else "vertical"

        val serviceIntent = Intent(this, GreenLineService::class.java).apply {
            putStringArrayListExtra("colors", selectedColors)
            putExtra("lineCount", lineCount)
            putExtra("orientation", orientation)
            putExtra("randomize", randomize)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            startService(serviceIntent)
            updateUiState(true)
        }
    }

    private fun updateUiState(isRunning: Boolean) {
        isServiceRunning = isRunning
        if (isRunning) {
            toggleButton.text = getString(R.string.stop_overlay)
            toggleButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
            randomizeButton.isEnabled = true
            greenCheckbox.isEnabled = false
            purpleCheckbox.isEnabled = false
            horizontalCheckbox.isEnabled = false
            lineCountSpinner.isEnabled = false
        } else {
            toggleButton.text = getString(R.string.start_overlay)
            toggleButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple_500)
            randomizeButton.isEnabled = false
            greenCheckbox.isEnabled = true
            purpleCheckbox.isEnabled = true
            horizontalCheckbox.isEnabled = true
            lineCountSpinner.isEnabled = true
        }
    }
}