package com.example.ui.clock

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockScreen(
    settings: AppSettings,
    batteryLevel: Int,
    isCharging: Boolean,
    onOpenFlashlight: () -> Unit,
    onOpenCalculator: () -> Unit,
    onTriggerSecretEntry: () -> Unit,
    onOpenAboutGuide: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    var showCustomizationDialog by remember { mutableStateOf(false) }

    val activeTimeZone = remember(settings.selectedTimeZoneId) {
        if (settings.selectedTimeZoneId.isNotBlank()) {
            try {
                TimeZone.getTimeZone(settings.selectedTimeZoneId)
            } catch (e: Exception) {
                TimeZone.getDefault()
            }
        } else {
            TimeZone.getDefault()
        }
    }

    LaunchedEffect(activeTimeZone) {
        while (true) {
            currentTime = Calendar.getInstance(activeTimeZone)
            delay(100L)
        }
    }

    val baseColor = try {
        Color(android.graphics.Color.parseColor(settings.clockColorHex))
    } catch (e: Exception) {
        Color(0xFF00E5FF)
    }

    val displayColor = if (settings.isNightMode) Color(0xFFFF4444).copy(alpha = 0.75f) else baseColor

    val timeFormatPattern = when {
        settings.clockIs24Hour && settings.clockShowSeconds -> "HH:mm:ss"
        settings.clockIs24Hour -> "HH:mm"
        settings.clockShowSeconds -> "hh:mm:ss a"
        else -> "hh:mm a"
    }

    val timeFormatter = remember(timeFormatPattern, activeTimeZone) {
        SimpleDateFormat(timeFormatPattern, Locale.getDefault()).apply {
            timeZone = activeTimeZone
        }
    }

    val datePattern = when (settings.dateFormatStyle) {
        "STANDARD" -> "EEE, d MMM yyyy"
        "COMPACT" -> "dd/MM/yyyy"
        "ISO" -> "yyyy-MM-dd"
        else -> "EEEE, MMMM d, yyyy"
    }

    val dateFormatter = remember(datePattern, activeTimeZone) {
        SimpleDateFormat(datePattern, Locale.getDefault()).apply {
            timeZone = activeTimeZone
        }
    }

    val fontFamily = when (settings.clockFontStyle) {
        "MONO" -> FontFamily.Monospace
        "SERIF" -> FontFamily.Serif
        else -> FontFamily.SansSerif
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (settings.isNightMode) Color(0xFF050505) else Color.Black)
            .padding(16.dp)
            .testTag("clock_screen_container")
    ) {
        // Top utility bar (Discreet utility app appearance)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Battery Status & Timezone tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        contentDescription = "Battery",
                        tint = if (batteryLevel < 20) Color(0xFFFF5252) else Color(0xFF88D49E),
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

                if (settings.timeZoneOffsetHours != 0 || settings.selectedTimeZoneLabel != "Auto (Device Time)") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E26))
                            .clickable { showCustomizationDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = settings.selectedTimeZoneLabel,
                            fontSize = 10.sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Actions: Calculator, Flashlight, Clock Settings, and Stealth Help/About
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenAboutGuide,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .testTag("clock_about_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "About Creator & Guide",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenCalculator,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .testTag("clock_to_calc_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculator",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenFlashlight,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .testTag("clock_to_flashlight_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashlightOn,
                        contentDescription = "Flashlight",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { showCustomizationDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .testTag("clock_customize_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "Clock Style",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Center Clock Display (Secret Gesture: Long press or double tap triggers vault unlock!)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (settings.isAnalogClock) {
                AnalogClock(
                    calendar = currentTime,
                    accentColor = displayColor,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onTriggerSecretEntry() },
                                onDoubleTap = { onTriggerSecretEntry() }
                            )
                        }
                        .testTag("analog_clock_view")
                )
            } else {
                val formattedTime = timeFormatter.format(currentTime.time)
                Text(
                    text = formattedTime,
                    fontSize = if (settings.clockShowSeconds) 50.sp else 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = displayColor,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onTriggerSecretEntry() },
                                onDoubleTap = { onTriggerSecretEntry() }
                            )
                        }
                        .testTag("digital_clock_text")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Formatted Date
            Text(
                text = dateFormatter.format(currentTime.time),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.LightGray,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { showCustomizationDialog = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tap to sync date/time • Long press digits to unlock",
                fontSize = 10.sp,
                color = Color.DarkGray
            )
        }

        // Bottom Controls: Night Mode, Analog/Digital, Keep Awake
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF121214).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF222226), RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = settings.isNightMode,
                onClick = {
                    onUpdateSettings(settings.copy(isNightMode = !settings.isNightMode))
                },
                label = { Text("Night", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF331111),
                    selectedLabelColor = Color(0xFFFF6666),
                    selectedLeadingIconColor = Color(0xFFFF6666)
                )
            )

            FilterChip(
                selected = settings.isAnalogClock,
                onClick = {
                    onUpdateSettings(settings.copy(isAnalogClock = !settings.isAnalogClock))
                },
                label = { Text(if (settings.isAnalogClock) "Analog" else "Digital", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )

            FilterChip(
                selected = settings.keepScreenOn,
                onClick = {
                    onUpdateSettings(settings.copy(keepScreenOn = !settings.keepScreenOn))
                },
                label = { Text("Awake", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (settings.keepScreenOn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }
    }

    // Customization & Date Synchronization Sheet/Dialog
    if (showCustomizationDialog) {
        ClockCustomizationDialog(
            currentSettings = settings,
            onDismiss = { showCustomizationDialog = false },
            onApply = { newSettings ->
                onUpdateSettings(newSettings)
                showCustomizationDialog = false
            }
        )
    }
}

@Composable
fun ClockCustomizationDialog(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onApply: (AppSettings) -> Unit
) {
    var is24h by remember { mutableStateOf(currentSettings.clockIs24Hour) }
    var showSeconds by remember { mutableStateOf(currentSettings.clockShowSeconds) }
    var fontStyle by remember { mutableStateOf(currentSettings.clockFontStyle) }
    var selectedColor by remember { mutableStateOf(currentSettings.clockColorHex) }
    var dateFormatStyle by remember { mutableStateOf(currentSettings.dateFormatStyle) }
    var timeZoneOffsetHours by remember { mutableIntStateOf(currentSettings.timeZoneOffsetHours) }
    var selectedTimeZoneLabel by remember { mutableStateOf(currentSettings.selectedTimeZoneLabel) }
    var selectedTimeZoneId by remember { mutableStateOf(currentSettings.selectedTimeZoneId) }

    val colorOptions = listOf(
        "#00E5FF" to "Cyan",
        "#00E676" to "Emerald",
        "#FFB300" to "Amber",
        "#7C4DFF" to "Violet",
        "#FFFFFF" to "White",
        "#FF1744" to "Red"
    )

    val fontOptions = listOf(
        "DIGITAL" to "Modern Sans",
        "MONO" to "Cyber Mono",
        "SERIF" to "Classic Serif"
    )

    val dateFormatOptions = listOf(
        "FULL" to "Full (Saturday, 7 November)",
        "STANDARD" to "Standard (Sat, 7 Nov 2026)",
        "COMPACT" to "Compact (07/11/2026)",
        "ISO" to "ISO (2026-11-07)"
    )

    // Accurate IANA Timezones to perfectly match real-world local dates & times
    val timezonePresets = listOf(
        Triple("Auto (Device Time)", "", 0),
        Triple("Pakistan Time (PKT UTC+5)", "Asia/Karachi", 5),
        Triple("India Time (IST UTC+5:30)", "Asia/Kolkata", 5),
        Triple("Gulf Time (GST UTC+4)", "Asia/Dubai", 4),
        Triple("UK / London (GMT/BST)", "Europe/London", 0),
        Triple("US East (EST/EDT)", "America/New_York", -5),
        Triple("US West (PST/PDT)", "America/Los_Angeles", -8)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Clock & Date Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        },
        containerColor = Color(0xFF16161A),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timezone & Date Synchronization
                Text("Date & Time Synchronization", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "If your device or emulator date/clock does not match your real location, select your timezone:",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    timezonePresets.forEach { (label, tzId, offset) ->
                        val isSelected = selectedTimeZoneLabel == label || (selectedTimeZoneId.isNotBlank() && selectedTimeZoneId == tzId)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTimeZoneLabel = label
                                    selectedTimeZoneId = tzId
                                    timeZoneOffsetHours = offset
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E2E3E) else Color(0xFF1C1C22)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = if (isSelected) Color(0xFF00E5FF) else Color.White, fontSize = 12.sp)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Date Format
                Text("Date Format Display", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dateFormatOptions.forEach { (fmt, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (dateFormatStyle == fmt) Color(0xFF1E2E3E) else Color(0xFF1C1C22))
                                .clickable { dateFormatStyle = fmt }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = Color.White, fontSize = 12.sp)
                            if (dateFormatStyle == fmt) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // 12/24 Hour toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("24-Hour Format", color = Color.LightGray, fontSize = 13.sp)
                    Switch(
                        checked = is24h,
                        onCheckedChange = { is24h = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }

                // Show Seconds toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Display Seconds", color = Color.LightGray, fontSize = 13.sp)
                    Switch(
                        checked = showSeconds,
                        onCheckedChange = { showSeconds = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }

                // Font selection
                Text("Typography", color = Color.LightGray, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontOptions.forEach { (style, label) ->
                        FilterChip(
                            selected = fontStyle == style,
                            onClick = { fontStyle = style },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Color Selection
                Text("Accent Color", color = Color.LightGray, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { (hex, _) ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 1.dp,
                                    color = if (selectedColor == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        currentSettings.copy(
                            clockIs24Hour = is24h,
                            clockShowSeconds = showSeconds,
                            clockFontStyle = fontStyle,
                            clockColorHex = selectedColor,
                            dateFormatStyle = dateFormatStyle,
                            timeZoneOffsetHours = timeZoneOffsetHours,
                            selectedTimeZoneLabel = selectedTimeZoneLabel,
                            selectedTimeZoneId = selectedTimeZoneId
                        )
                    )
                }
            ) {
                Text("Apply & Sync", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
