package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SecretPinDialog(
    onDismiss: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onForgotPin: () -> Unit,
    onSetupNewPin: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Security Verification",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter your secret PIN to access UV Vault",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = 4
                    for (i in 0 until dotCount) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) Color(0xFF00E5FF) else Color(0xFF262630))
                                .border(
                                    width = 1.dp,
                                    color = if (isFilled) Color(0xFF00E5FF) else Color(0xFF3E3E4C),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Numeric Keypad
                val keypadButtons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("FORGOT", "0", "DEL")
                )

                for (row in keypadButtons) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (key) {
                                            "FORGOT" -> Color.Transparent
                                            "DEL" -> Color(0xFF1E1E26)
                                            else -> Color(0xFF1A1A22)
                                        }
                                    )
                                    .clickable {
                                        when (key) {
                                            "DEL" -> {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            }
                                            "FORGOT" -> onForgotPin()
                                            else -> {
                                                if (pin.length < 8) {
                                                    val newPin = pin + key
                                                    pin = newPin
                                                    if (newPin.length >= 4) {
                                                        onSubmitPin(newPin)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "DEL" -> Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    "FORGOT" -> Text(
                                        text = "Forgot?",
                                        fontSize = 12.sp,
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Medium
                                    )
                                    else -> Text(
                                        text = key,
                                        fontSize = 22.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onSetupNewPin) {
                    Text("Create or Reset Master PIN", color = Color(0xFF00E5FF), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CreateMasterPinDialog(
    onDismiss: () -> Unit,
    onPinCreated: (pin: String, question: String, answer: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) } // 1: Enter PIN, 2: Confirm PIN, 3: Recovery Question
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryQuestion by remember { mutableStateOf("What is your secret phrase?") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0xFF24242C), RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Set Up Master PIN",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        Box(
                            modifier = Modifier
                                .size(if (step == i) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (step >= i) Color(0xFF00E5FF) else Color(0xFF33333E))
                        )
                        if (i < 3) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(2.dp)
                                    .background(if (step > i) Color(0xFF00E5FF) else Color(0xFF33333E))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                when (step) {
                    1 -> {
                        Text(
                            text = "Create Your Secret PIN",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Choose 4 to 8 digits you will use to unlock your vault via the Clock or Calculator",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        // Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (i in 0 until 4) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (i < firstPin.length) Color(0xFF00E5FF) else Color(0xFF262630))
                                )
                            }
                        }

                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(errorMsg ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Keypad for step 1
                        PinKeypad(
                            currentPin = firstPin,
                            onPinChange = {
                                firstPin = it
                                errorMsg = null
                                if (it.length >= 4) {
                                    // Can advance
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (firstPin.length < 4) {
                                    errorMsg = "PIN must be at least 4 digits"
                                } else {
                                    errorMsg = null
                                    step = 2
                                }
                            },
                            enabled = firstPin.length >= 4,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next: Confirm PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    2 -> {
                        Text(
                            text = "Confirm Your Secret PIN",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Re-enter the same PIN to make sure there are no typos",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        // Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (i in 0 until 4) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (i < confirmPin.length) Color(0xFF00E5FF) else Color(0xFF262630))
                                )
                            }
                        }

                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(errorMsg ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Keypad for step 2
                        PinKeypad(
                            currentPin = confirmPin,
                            onPinChange = {
                                confirmPin = it
                                errorMsg = null
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    step = 1
                                    confirmPin = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back", color = Color.LightGray)
                            }

                            Button(
                                onClick = {
                                    if (confirmPin != firstPin) {
                                        errorMsg = "PINs do not match. Please try again."
                                    } else {
                                        errorMsg = null
                                        step = 3
                                    }
                                },
                                enabled = confirmPin.length >= 4,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next: Recovery", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = "Security Recovery Phrase",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Set a security question so you can recover your PIN if you ever forget it",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = recoveryQuestion,
                            onValueChange = { recoveryQuestion = it },
                            label = { Text("Security Question") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = { recoveryAnswer = it },
                            label = { Text("Secret Answer") },
                            placeholder = { Text("e.g. My secret key or nickname") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(errorMsg ?: "", color = Color(0xFFFF5252), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (recoveryAnswer.isBlank()) {
                                    errorMsg = "Please enter a secret answer"
                                } else {
                                    onPinCreated(firstPin, recoveryQuestion.trim(), recoveryAnswer.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Complete Setup & Open Vault", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinKeypad(
    currentPin: String,
    onPinChange: (String) -> Unit
) {
    val keypadButtons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLEAR", "0", "DEL")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in keypadButtons) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                when (key) {
                                    "CLEAR" -> Color.Transparent
                                    "DEL" -> Color(0xFF1E1E26)
                                    else -> Color(0xFF1A1A22)
                                }
                            )
                            .clickable {
                                when (key) {
                                    "CLEAR" -> onPinChange("")
                                    "DEL" -> if (currentPin.isNotEmpty()) onPinChange(currentPin.dropLast(1))
                                    else -> if (currentPin.length < 8) onPinChange(currentPin + key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "DEL" -> Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                            "CLEAR" -> Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            else -> Text(
                                text = key,
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecoveryDialog(
    question: String,
    onDismiss: () -> Unit,
    onSubmitAnswer: (String) -> Unit
) {
    var answer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security Recovery", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(question, color = Color.LightGray, fontSize = 14.sp)
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Your Secret Answer") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitAnswer(answer) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Verify & Reset PIN", color = Color.Black, fontWeight = FontWeight.Bold)
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
