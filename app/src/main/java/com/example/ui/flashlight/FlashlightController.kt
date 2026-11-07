package com.example.ui.flashlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FlashlightMode {
    SOLID,
    STROBE,
    SOS,
    SCREEN_LIGHT
}

class FlashlightController(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null
    private var hasHardwareFlash: Boolean = false
    private var maxTorchStrength: Int = 1

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _flashlightMode = MutableStateFlow(FlashlightMode.SOLID)
    val flashlightMode: StateFlow<FlashlightMode> = _flashlightMode.asStateFlow()

    private val _strobeHz = MutableStateFlow(4f)
    val strobeHz: StateFlow<Float> = _strobeHz.asStateFlow()

    private val _torchStrength = MutableStateFlow(1f) // 0f to 1f
    val torchStrength: StateFlow<Float> = _torchStrength.asStateFlow()

    private val _hasHardwareFlashFlow = MutableStateFlow(false)
    val hasHardwareFlashFlow: StateFlow<Boolean> = _hasHardwareFlashFlow.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private var activeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(camId: String, enabled: Boolean) {
            if (camId == cameraId) {
                _isTorchOn.value = enabled
            }
        }

        override fun onTorchModeUnavailable(camId: String) {
            if (camId == cameraId) {
                _isTorchOn.value = false
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    _batteryLevel.value = (level * 100) / scale
                }
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    init {
        detectHardwareFlash()
        try {
            cameraManager?.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) {
            Log.e("FlashlightController", "Torch callback registration error", e)
        }
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("FlashlightController", "Battery receiver registration error", e)
        }
    }

    private fun detectHardwareFlash() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    hasHardwareFlash = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        maxTorchStrength = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    }
                    _hasHardwareFlashFlow.value = true
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("FlashlightController", "Detect hardware flash failed", e)
        }
    }

    fun setMode(mode: FlashlightMode) {
        _flashlightMode.value = mode
        if (_isTorchOn.value) {
            restartCurrentMode()
        }
    }

    fun setStrobeHz(hz: Float) {
        _strobeHz.value = hz
        if (_isTorchOn.value && _flashlightMode.value == FlashlightMode.STROBE) {
            restartCurrentMode()
        }
    }

    fun setTorchStrength(strength: Float) {
        _torchStrength.value = strength.coerceIn(0.1f, 1f)
        if (_isTorchOn.value && _flashlightMode.value == FlashlightMode.SOLID) {
            applyHardwareTorch(true)
        }
    }

    fun toggleTorch() {
        if (_isTorchOn.value) {
            turnOff()
        } else {
            turnOn()
        }
    }

    fun turnOn() {
        _isTorchOn.value = true
        restartCurrentMode()
    }

    fun turnOff() {
        activeJob?.cancel()
        activeJob = null
        _isTorchOn.value = false
        applyHardwareTorch(false)
    }

    private fun restartCurrentMode() {
        activeJob?.cancel()
        when (_flashlightMode.value) {
            FlashlightMode.SOLID -> {
                applyHardwareTorch(true)
            }
            FlashlightMode.STROBE -> {
                activeJob = scope.launch {
                    while (isActive) {
                        val hz = _strobeHz.value.coerceIn(1f, 15f)
                        val periodMs = (1000L / hz).toLong()
                        val halfPeriod = (periodMs / 2).coerceAtLeast(20L)
                        applyHardwareTorch(true)
                        delay(halfPeriod)
                        applyHardwareTorch(false)
                        delay(halfPeriod)
                    }
                }
            }
            FlashlightMode.SOS -> {
                activeJob = scope.launch {
                    val dot = 150L
                    val dash = 450L
                    val intraChar = 150L
                    val charGap = 450L
                    val wordGap = 1200L

                    while (isActive) {
                        // S: 3 dots
                        repeat(3) {
                            applyHardwareTorch(true)
                            delay(dot)
                            applyHardwareTorch(false)
                            delay(intraChar)
                        }
                        delay(charGap)

                        // O: 3 dashes
                        repeat(3) {
                            applyHardwareTorch(true)
                            delay(dash)
                            applyHardwareTorch(false)
                            delay(intraChar)
                        }
                        delay(charGap)

                        // S: 3 dots
                        repeat(3) {
                            applyHardwareTorch(true)
                            delay(dot)
                            applyHardwareTorch(false)
                            delay(intraChar)
                        }
                        delay(wordGap)
                    }
                }
            }
            FlashlightMode.SCREEN_LIGHT -> {
                applyHardwareTorch(false)
            }
        }
    }

    private fun applyHardwareTorch(enabled: Boolean) {
        val id = cameraId ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxTorchStrength > 1 && enabled) {
                val level = (_torchStrength.value * maxTorchStrength).toInt().coerceIn(1, maxTorchStrength)
                cameraManager?.turnOnTorchWithStrengthLevel(id, level)
            } else {
                cameraManager?.setTorchMode(id, enabled)
            }
        } catch (e: Exception) {
            Log.e("FlashlightController", "Hardware torch toggle error", e)
        }
    }

    fun destroy() {
        turnOff()
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            // Ignore
        }
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
