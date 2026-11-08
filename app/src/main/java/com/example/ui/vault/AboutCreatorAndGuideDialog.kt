package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AboutCreatorAndGuideDialog(
    onDismiss: () -> Unit,
    initialTab: Int = 0 // 0: Creator, 1: Guide
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0xFF262632), RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .testTag("about_creator_dialog")
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UV PRIVACY",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Creator & Secret Guide
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1C1C24),
                    contentColor = Color(0xFF00E5FF),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("About Creator", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Secret Guide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        // Creator Profile Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181820)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Creator",
                                        tint = Color.Black,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Darya Khan abro",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Text(
                                    text = "team Mr Pro PrimeX",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF00E5FF)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                AssistChip(
                                    onClick = {},
                                    label = { Text("Lead Developer & Architect", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFF22222E),
                                        labelColor = Color.LightGray
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFF262632))
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "UV PRIVACY was conceptualized and engineered by Darya Khan abro and team Mr Pro PrimeX to deliver military-grade zero-knowledge privacy disguised as a clock and flashlight utility.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD0D0D8),
                                    lineHeight = 18.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // App Highlights
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181820)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Engineering Highlights", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                HighlightRow(Icons.Default.Lock, "AES-256-GCM authenticated file encryption")
                                HighlightRow(Icons.Default.Key, "PBKDF2WithHmacSHA256 (65,536 iterations)")
                                HighlightRow(Icons.Default.Masks, "Dual disguise: Functional Clock & Torch")
                                HighlightRow(Icons.Default.Calculate, "Secret Calculator PIN unlock entry")
                                HighlightRow(Icons.Default.Schedule, "Timezone & Date synchronization engine")
                            }
                        }
                    } else {
                        // How To Use & Secret Guide
                        Text("Secret Vault Entry Methods", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        GuideStepCard(
                            icon = Icons.Default.Schedule,
                            title = "1. Clock Secret Gesture",
                            description = "On the clock screen, long-press the clock digits or double-tap the center colon ':' to pop up the Master PIN dialog."
                        )

                        GuideStepCard(
                            icon = Icons.Default.Calculate,
                            title = "2. Fake Calculator Entry",
                            description = "Tap the calculator icon on the clock. Type your Secret PIN and press '='. If the PIN matches, your vault opens immediately! Otherwise it behaves as a normal calculator."
                        )

                        GuideStepCard(
                            icon = Icons.Default.ShieldMoon,
                            title = "3. Decoy Vault Protection",
                            description = "Enable Decoy Vault in Settings and choose a separate Decoy PIN. If forced to unlock your phone, enter the Decoy PIN to open an isolated, dummy vault."
                        )

                        GuideStepCard(
                            icon = Icons.Default.FlashlightOn,
                            title = "4. Flashlight Secret Vault Access",
                            description = "Slide the Brightness Output slider to 77% and release, or long-press the Torch Power button for 1.5 seconds, or double-tap 'Brightness Output' to immediately reveal the Secret PIN screen!"
                        )

                        GuideStepCard(
                            icon = Icons.Default.AccessTime,
                            title = "5. Date & Time Matching",
                            description = "If the emulator or device time/date does not match your region, tap the Palette icon on the clock and select your Timezone offset to sync date and time perfectly!"
                        )

                        GuideStepCard(
                            icon = Icons.Default.Backup,
                            title = "6. Encrypted Backups",
                            description = "Create password-protected encrypted archives of all your private files in Settings to transfer or restore anytime safely."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GuideStepCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181820)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun HighlightRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
        Text(text, color = Color.LightGray, fontSize = 12.sp)
    }
}
