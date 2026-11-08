package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultCategory {
    PHOTO,
    VIDEO,
    DOCUMENT,
    OTHER;

    companion object {
        fun fromExtension(ext: String): VaultCategory {
            val lower = ext.lowercase()
            return when {
                lower in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg") -> PHOTO
                lower in listOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "flv") -> VIDEO
                lower in listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "json") -> DOCUMENT
                else -> OTHER
            }
        }
    }
}

enum class VaultAccessMethod {
    CLOCK_LONG_PRESS,
    CLOCK_COLON_DOUBLE_TAP,
    FAKE_CALCULATOR,
    SECRET_GESTURE
}

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val originalFileName: String,
    val encryptedFilePath: String,
    val fileSize: Long,
    val category: VaultCategory,
    val mimeType: String,
    val folderId: Long? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val importedTimestamp: Long = System.currentTimeMillis(),
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isDecoy: Boolean = false
)

@Entity(tableName = "vault_folders")
data class VaultFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: VaultCategory,
    val colorHex: String = "#00D2FF",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isDecoy: Boolean = false
)

@Entity(tableName = "intruder_logs")
data class IntruderLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptType: String,
    val details: String
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val masterPinHash: String = "",
    val decoyPinHash: String = "",
    val isDecoyEnabled: Boolean = false,
    val recoveryQuestion: String = "What is your secret phrase?",
    val recoveryAnswerHash: String = "",
    val accessMethod: VaultAccessMethod = VaultAccessMethod.CLOCK_LONG_PRESS,
    val autoLockSeconds: Int = 60, // 0 = immediate, 60 = 1 min
    val lockOnBackground: Boolean = true,
    val clockIs24Hour: Boolean = true,
    val clockShowSeconds: Boolean = true,
    val clockFontStyle: String = "DIGITAL",
    val clockColorHex: String = "#00F0FF",
    val isAnalogClock: Boolean = false,
    val isNightMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val isPinConfigured: Boolean = false,
    val dateFormatStyle: String = "FULL",
    val showDateTz: Boolean = false,
    val timeZoneOffsetHours: Int = 0,
    val selectedTimeZoneLabel: String = "Auto (Device Time)",
    val selectedTimeZoneId: String = "",
    val flashlightDefaultStrobeHz: Float = 3f,
    val flashlightScreenColorHex: String = "#FFFFFF",
    val hapticFeedback: Boolean = true,
    val soundFeedback: Boolean = true,
    val promptDeleteOriginals: Boolean = true
)
