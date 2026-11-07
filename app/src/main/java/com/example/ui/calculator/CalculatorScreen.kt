package com.example.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun CalculatorScreen(
    onEvaluateSecret: (String) -> Boolean,
    onBackToClock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayText by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var shouldClearOnNextDigit by remember { mutableStateOf(false) }

    fun onDigit(digit: String) {
        if (displayText == "0" || shouldClearOnNextDigit) {
            displayText = digit
            shouldClearOnNextDigit = false
        } else {
            if (displayText.length < 12) {
                displayText += digit
            }
        }
    }

    fun onDot() {
        if (shouldClearOnNextDigit) {
            displayText = "0."
            shouldClearOnNextDigit = false
        } else if (!displayText.contains(".")) {
            displayText += "."
        }
    }

    fun onClear() {
        displayText = "0"
        operand1 = null
        pendingOp = null
        shouldClearOnNextDigit = false
    }

    fun onOperator(op: String) {
        operand1 = displayText.toDoubleOrNull()
        pendingOp = op
        shouldClearOnNextDigit = true
    }

    fun onEquals() {
        // First check secret entry with current display string
        val cleanCandidate = displayText.trim()
        val unlocked = onEvaluateSecret(cleanCandidate)
        if (unlocked) {
            return
        }

        // If not unlocked, perform regular calculation
        val op = pendingOp
        val first = operand1
        val second = displayText.toDoubleOrNull()

        if (op != null && first != null && second != null) {
            val result = when (op) {
                "+" -> first + second
                "-" -> first - second
                "×" -> first * second
                "÷" -> if (second == 0.0) Double.NaN else first / second
                else -> second
            }

            displayText = if (result.isNaN()) {
                "Error"
            } else {
                val format = DecimalFormat("#.########")
                format.format(result)
            }
            operand1 = null
            pendingOp = null
            shouldClearOnNextDigit = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("calculator_screen_container")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToClock,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16161A))
                        .testTag("calc_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Clock",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Calculator",
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        if (displayText.length > 1) {
                            displayText = displayText.dropLast(1)
                        } else {
                            displayText = "0"
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16161A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Delete digit",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Calculation Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = displayText,
                    fontSize = if (displayText.length > 8) 42.sp else 64.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.testTag("calc_display_text")
                )
            }

            // Keypad Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val buttonRows = listOf(
                    listOf("C" to Color(0xFFA5A5A5), "+/-" to Color(0xFFA5A5A5), "%" to Color(0xFFA5A5A5), "÷" to Color(0xFFFF9F0A)),
                    listOf("7" to Color(0xFF333333), "8" to Color(0xFF333333), "9" to Color(0xFF333333), "×" to Color(0xFFFF9F0A)),
                    listOf("4" to Color(0xFF333333), "5" to Color(0xFF333333), "6" to Color(0xFF333333), "-" to Color(0xFFFF9F0A)),
                    listOf("1" to Color(0xFF333333), "2" to Color(0xFF333333), "3" to Color(0xFF333333), "+" to Color(0xFFFF9F0A)),
                    listOf("0" to Color(0xFF333333), "." to Color(0xFF333333), "=" to Color(0xFFFF9F0A))
                )

                for ((rowIndex, row) in buttonRows.withIndex()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (item in row) {
                            val (label, bgColor) = item
                            val isZero = label == "0" && rowIndex == 4
                            val textColor = if (bgColor == Color(0xFFA5A5A5)) Color.Black else Color.White

                            Box(
                                modifier = Modifier
                                    .weight(if (isZero) 2f else 1f)
                                    .aspectRatio(if (isZero) 2.1f else 1f)
                                    .clip(if (isZero) RoundedCornerShape(40.dp) else CircleShape)
                                    .background(bgColor)
                                    .clickable {
                                        when (label) {
                                            "C" -> onClear()
                                            "+/-" -> {
                                                if (displayText != "0") {
                                                    displayText = if (displayText.startsWith("-")) displayText.substring(1) else "-$displayText"
                                                }
                                            }
                                            "%" -> {
                                                val cleanCandidate = displayText.trim()
                                                val unlocked = onEvaluateSecret(cleanCandidate)
                                                if (!unlocked) {
                                                    val v = displayText.toDoubleOrNull()
                                                    if (v != null) {
                                                        displayText = (v / 100).toString()
                                                    }
                                                }
                                            }
                                            "÷", "×", "-", "+" -> onOperator(label)
                                            "=" -> onEquals()
                                            "." -> onDot()
                                            else -> onDigit(label)
                                        }
                                    }
                                    .testTag("calc_btn_$label"),
                                contentAlignment = if (isZero) Alignment.CenterStart else Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor,
                                    modifier = if (isZero) Modifier.padding(start = 28.dp) else Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
