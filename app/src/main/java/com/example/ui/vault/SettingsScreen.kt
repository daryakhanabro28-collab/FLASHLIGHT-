package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.CryptoManager
import com.example.data.model.AppSettings
import com.example.data.model.VaultAccessMethod

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .padding(16.dp)
            .testTag("vault_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Access & PIN
        item {
            SettingsCategoryCard("Security & Access") {
                SettingsActionRow(
                    icon = Icons.Default.Password,
                    title = "Change Master PIN",
                    subtitle = "Update PIN used to unlock your primary vault",
                    onClick = { showChangePinDialog = true }
                )

                HorizontalDivider(color = Color(0xFF1E1E26))

                SettingsActionRow(
                    icon = Icons.Default.Security,
                    title = "Secret Access Trigger",
                    subtitle = when (settings.accessMethod) {
                        VaultAccessMethod.CLOCK_LONG_PRESS -> "Clock Long Press"
                        VaultAccessMethod.CLOCK_COLON_DOUBLE_TAP -> "Clock Double Tap"
                        VaultAccessMethod.FAKE_CALCULATOR -> "Calculator PIN + ="
                        VaultAccessMethod.SECRET_GESTURE -> "Hidden Gesture"
                    },
                    onClick = {
                        val next = when (settings.accessMethod) {
                            VaultAccessMethod.CLOCK_LONG_PRESS -> VaultAccessMethod.FAKE_CALCULATOR
                            VaultAccessMethod.FAKE_CALCULATOR -> VaultAccessMethod.CLOCK_COLON_DOUBLE_TAP
                            VaultAccessMethod.CLOCK_COLON_DOUBLE_TAP -> VaultAccessMethod.CLOCK_LONG_PRESS
                            else -> VaultAccessMethod.CLOCK_LONG_PRESS
                        }
                        onUpdateSettings(settings.copy(accessMethod = next))
                    }
                )

                HorizontalDivider(color = Color(0xFF1E1E26))

                SettingsActionRow(
                    icon = Icons.Default.QuestionAnswer,
                    title = "Recovery Phrase",
                    subtitle = "Set security question to recover PIN if forgotten",
                    onClick = { showRecoveryDialog = true }
                )
            }
        }

        // Section: Decoy Vault
        item {
            SettingsCategoryCard("Decoy Vault System") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Decoy Vault", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("A separate decoy PIN opens a dummy vault that conceals your real files", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.isDecoyEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && settings.decoyPinHash.isEmpty()) {
                                showDecoyPinDialog = true
                            } else {
                                onUpdateSettings(settings.copy(isDecoyEnabled = enabled))
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }

                if (settings.isDecoyEnabled) {
                    HorizontalDivider(color = Color(0xFF1E1E26))
                    SettingsActionRow(
                        icon = Icons.Default.Pin,
                        title = "Set Decoy PIN",
                        subtitle = if (settings.decoyPinHash.isEmpty()) "Not set (Tap to configure)" else "Configured",
                        onClick = { showDecoyPinDialog = true }
                    )
                }
            }
        }

        // Section: Auto-Lock & App Protection
        item {
            SettingsCategoryCard("Auto-Lock & Background Policy") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lock on Background", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Immediately wipe keys and lock when app is minimized", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.lockOnBackground,
                        onCheckedChange = { onUpdateSettings(settings.copy(lockOnBackground = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }

                HorizontalDivider(color = Color(0xFF1E1E26))

                SettingsActionRow(
                    icon = Icons.Default.Timer,
                    title = "Inactivity Auto-Lock",
                    subtitle = "${settings.autoLockSeconds} seconds of idle time",
                    onClick = {
                        val nextSeconds = when (settings.autoLockSeconds) {
                            0 -> 30
                            30 -> 60
                            60 -> 120
                            120 -> 300
                            else -> 0
                        }
                        onUpdateSettings(settings.copy(autoLockSeconds = nextSeconds))
                    }
                )
            }
        }

        // Section: File Storage & Maintenance
        item {
            SettingsCategoryCard("Storage & Backup") {
                SettingsActionRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "Recycle Bin",
                    subtitle = "Manage and restore recently deleted files",
                    onClick = onOpenRecycleBin
                )

                HorizontalDivider(color = Color(0xFF1E1E26))

                SettingsActionRow(
                    icon = Icons.Default.Backup,
                    title = "Encrypted Backup & Restore",
                    subtitle = "Export or restore password-encrypted archive bundle",
                    onClick = onOpenBackup
                )
            }
        }

        // Section: Creator & How to Use
        item {
            SettingsCategoryCard("Creator & Stealth Guide") {
                SettingsActionRow(
                    icon = Icons.Default.Person,
                    title = "About Creator: Darya Khan abro",
                    subtitle = "team Mr Pro PrimeX • Lead Developer & Architect",
                    onClick = { showAboutDialog = true }
                )

                HorizontalDivider(color = Color(0xFF1E1E26))

                SettingsActionRow(
                    icon = Icons.Default.MenuBook,
                    title = "Secret How-To-Use Guide",
                    subtitle = "Hidden gestures, calculator PIN entry & stealth tips",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    // Change Master PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            title = "Change Master PIN",
            onDismiss = { showChangePinDialog = false },
            onSavePin = { newPin ->
                val salt = CryptoManager.generateRandomSalt()
                val hash = CryptoManager.hashPin(newPin, salt)
                onUpdateSettings(settings.copy(masterPinHash = hash))
                showChangePinDialog = false
            }
        )
    }

    // Set Decoy PIN Dialog
    if (showDecoyPinDialog) {
        ChangePinDialog(
            title = "Set Decoy PIN",
            onDismiss = { showDecoyPinDialog = false },
            onSavePin = { newPin ->
                val salt = CryptoManager.generateRandomSalt()
                val hash = CryptoManager.hashPin(newPin, salt)
                onUpdateSettings(settings.copy(decoyPinHash = hash, isDecoyEnabled = true))
                showDecoyPinDialog = false
            }
        )
    }

    // Recovery Setup Dialog
    if (showRecoveryDialog) {
        RecoverySetupDialog(
            currentQuestion = settings.recoveryQuestion,
            onDismiss = { showRecoveryDialog = false },
            onSave = { q, a ->
                val salt = CryptoManager.generateRandomSalt()
                val hash = CryptoManager.hashPin(a.lowercase().trim(), salt)
                onUpdateSettings(settings.copy(recoveryQuestion = q, recoveryAnswerHash = hash))
                showRecoveryDialog = false
            }
        )
    }

    // About & Secret Guide Dialog
    if (showAboutDialog) {
        AboutCreatorAndGuideDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
fun SettingsCategoryCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
            shape = RoundedCornerShape(16.dp),
            content = content
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ChangePinDialog(
    title: String,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it },
                    label = { Text("New PIN (4-8 digits)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 8) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        error = "PIN must be at least 4 digits"
                    } else if (pin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onSavePin(pin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF16161A)
    )
}

@Composable
fun RecoverySetupDialog(
    currentQuestion: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var question by remember { mutableStateOf(currentQuestion) }
    var answer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Recovery", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Security Question") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Secret Answer") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (answer.isNotBlank()) {
                        onSave(question.trim(), answer.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF16161A)
    )
}
