package com.example.ui.flashlight

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FlashlightScreen(
    controller: FlashlightController,
    onBackToClock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTorchOn by controller.isTorchOn.collectAsState()
    val mode by controller.flashlightMode.collectAsState()
    val strobeHz by controller.strobeHz.collectAsState()
    val strength by controller.torchStrength.collectAsState()
    val batteryLevel by controller.batteryLevel.collectAsState()
    val isCharging by controller.isCharging.collectAsState()
    val hasHardwareFlash by controller.hasHardwareFlashFlow.collectAsState()

    var screenColorHex by remember { mutableStateOf("#FFFFFF") }
    var isPoliceFlash by remember { mutableStateOf(false) }
    var policeToggle by remember { mutableStateOf(false) }

    LaunchedEffect(isPoliceFlash, isTorchOn) {
        if (isPoliceFlash && isTorchOn) {
            while (true) {
                policeToggle = !policeToggle
                delay(120L)
            }
        }
    }

    val currentScreenLightColor = when {
        isPoliceFlash && policeToggle -> Color(0xFFFF1744)
        isPoliceFlash -> Color(0xFF2979FF)
        else -> try {
            Color(android.graphics.Color.parseColor(screenColorHex))
        } catch (e: Exception) {
            Color.White
        }
    }

    // Full screen screen-light mode overlay
    if (mode == FlashlightMode.SCREEN_LIGHT && isTorchOn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentScreenLightColor)
                .pointerInput(Unit) {
                    detectTapGestures {
                        controller.turnOff()
                    }
                }
                .testTag("fullscreen_torch_overlay")
        ) {
            Text(
                text = "Tap anywhere to turn off",
                color = if (screenColorHex == "#FFFFFF" && !isPoliceFlash) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTorchOn) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beamPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .testTag("flashlight_screen_container")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToClock,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16161A))
                    .testTag("flashlight_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Clock",
                    tint = Color.White
                )
            }

            // Battery status & indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16161A))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    contentDescription = null,
                    tint = if (batteryLevel < 20) Color(0xFFFF5252) else Color(0xFFFFB300),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$batteryLevel%",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Main Torch Center Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mode Selectors
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141418))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    FlashlightMode.SOLID to "Torch",
                    FlashlightMode.STROBE to "Strobe",
                    FlashlightMode.SOS to "S.O.S",
                    FlashlightMode.SCREEN_LIGHT to "Screen"
                )
                modes.forEach { (m, label) ->
                    val isSelected = mode == m
                    FilterChip(
                        selected = isSelected,
                        onClick = { controller.setMode(m) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFB300),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Glowing Power Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Background aura
                if (isTorchOn) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFB300).copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                FilledIconButton(
                    onClick = { controller.toggleTorch() },
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            color = if (isTorchOn) Color(0xFFFFB300) else Color(0xFF2A2A30),
                            shape = CircleShape
                        )
                        .testTag("flashlight_power_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isTorchOn) Color(0xFFFFB300) else Color(0xFF1C1C22)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Torch",
                        modifier = Modifier.size(54.dp),
                        tint = if (isTorchOn) Color.Black else Color(0xFF888892)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isTorchOn) "ACTIVE" else "OFFLINE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (isTorchOn) Color(0xFFFFB300) else Color.Gray
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Context Controls based on Mode
            AnimatedVisibility(visible = mode == FlashlightMode.STROBE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Strobe Frequency", color = Color.White, fontSize = 13.sp)
                            Text("${strobeHz.toInt()} Hz", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = strobeHz,
                            onValueChange = { controller.setStrobeHz(it) },
                            valueRange = 1f..12f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300)
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(visible = mode == FlashlightMode.SOLID) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Brightness Output", color = Color.White, fontSize = 13.sp)
                            Text("${(strength * 100).toInt()}%", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = strength,
                            onValueChange = { controller.setTorchStrength(it) },
                            valueRange = 0.1f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFB300),
                                activeTrackColor = Color(0xFFFFB300)
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(visible = mode == FlashlightMode.SCREEN_LIGHT) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Screen Light Color", color = Color.White, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val colors = listOf(
                                "#FFFFFF" to "White",
                                "#FFB300" to "Amber",
                                "#FF1744" to "Red",
                                "#00E5FF" to "Cyan",
                                "#00E676" to "Green"
                            )
                            colors.forEach { (hex, _) ->
                                val col = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (screenColorHex == hex && !isPoliceFlash) 2.5.dp else 1.dp,
                                            color = if (screenColorHex == hex && !isPoliceFlash) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures {
                                                isPoliceFlash = false
                                                screenColorHex = hex
                                            }
                                        }
                                )
                            }

                            // Police flash toggle
                            FilterChip(
                                selected = isPoliceFlash,
                                onClick = { isPoliceFlash = !isPoliceFlash },
                                label = { Text("Police", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}
