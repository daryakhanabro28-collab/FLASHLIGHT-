package com.example.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.db.CategoryStorageStat
import com.example.data.model.AppSettings
import com.example.data.model.IntruderLog
import com.example.data.model.VaultCategory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityDashboardScreen(
    settings: AppSettings,
    storageStats: List<CategoryStorageStat>,
    totalSizeBytes: Long,
    totalFiles: Int,
    intruderLogs: List<IntruderLog>,
    onClearIntruderLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .padding(16.dp)
            .testTag("security_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text("VAULT PROTECTED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("AES-256-GCM Active", color = Color(0xFF00E676), fontSize = 12.sp)
                            }
                        }

                        AssistChip(
                            onClick = {},
                            label = { Text("ENCRYPTED", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF1F2F24),
                                labelColor = Color(0xFF00E676)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF24242C))
                    Spacer(modifier = Modifier.height(14.dp))

                    DetailRow("Cipher Specification", "AES-256-GCM Authenticated")
                    DetailRow("Key Derivation", "PBKDF2WithHmacSHA256 (65,536 iters)")
                    DetailRow("Random Salt & Nonce", "Unique 12-byte IV per file")
                    DetailRow("Auto-Lock Timeout", "${settings.autoLockSeconds}s inactivity")
                    DetailRow("Background Lock", if (settings.lockOnBackground) "Enabled" else "Disabled")
                    DetailRow("Decoy Vault", if (settings.isDecoyEnabled) "Configured & Active" else "Disabled")
                }
            }
        }

        // Storage Analyzer Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Encrypted Storage Analyzer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(formatFileSize(totalSizeBytes), color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text("Total items secured: $totalFiles", color = Color.Gray, fontSize = 12.sp)

                    // Multi-segment progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22222A))
                    ) {
                        val photoBytes = storageStats.firstOrNull { it.category == VaultCategory.PHOTO }?.totalBytes ?: 0L
                        val videoBytes = storageStats.firstOrNull { it.category == VaultCategory.VIDEO }?.totalBytes ?: 0L
                        val docBytes = storageStats.firstOrNull { it.category == VaultCategory.DOCUMENT }?.totalBytes ?: 0L
                        val otherBytes = storageStats.firstOrNull { it.category == VaultCategory.OTHER }?.totalBytes ?: 0L

                        val total = if (totalSizeBytes > 0) totalSizeBytes.toFloat() else 1f

                        if (photoBytes > 0) {
                            Box(modifier = Modifier.weight(photoBytes / total).fillMaxHeight().background(Color(0xFF00E5FF)))
                        }
                        if (videoBytes > 0) {
                            Box(modifier = Modifier.weight(videoBytes / total).fillMaxHeight().background(Color(0xFF7C4DFF)))
                        }
                        if (docBytes > 0) {
                            Box(modifier = Modifier.weight(docBytes / total).fillMaxHeight().background(Color(0xFFFFB300)))
                        }
                        if (otherBytes > 0) {
                            Box(modifier = Modifier.weight(otherBytes / total).fillMaxHeight().background(Color(0xFF00E676)))
                        }
                    }

                    // Category breakdown rows
                    StorageStatRow("Photos", Color(0xFF00E5FF), storageStats.firstOrNull { it.category == VaultCategory.PHOTO })
                    StorageStatRow("Videos", Color(0xFF7C4DFF), storageStats.firstOrNull { it.category == VaultCategory.VIDEO })
                    StorageStatRow("Documents", Color(0xFFFFB300), storageStats.firstOrNull { it.category == VaultCategory.DOCUMENT })
                    StorageStatRow("Other Files", Color(0xFF00E676), storageStats.firstOrNull { it.category == VaultCategory.OTHER })
                }
            }
        }

        // Intruder Event Log
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            Text("Intruder Event Log", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        if (intruderLogs.isNotEmpty()) {
                            TextButton(onClick = onClearIntruderLogs) {
                                Text("Clear", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (intruderLogs.isEmpty()) {
                        Text("No failed intrusion attempts detected. Your vault is secure.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            intruderLogs.take(5).forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A1A22))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.attemptType, color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(log.details, color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Text(
                                        dateFormat.format(Date(log.timestamp)),
                                        color = Color.DarkGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageStatRow(name: String, color: Color, stat: CategoryStorageStat?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Text(name, color = Color.LightGray, fontSize = 12.sp)
        }
        Text(
            text = "${stat?.count ?: 0} items • ${formatFileSize(stat?.totalBytes ?: 0L)}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
