package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.crypto.CryptoManager
import com.example.data.db.VaultDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao
) {
    val settingsFlow: Flow<AppSettings?> = dao.getSettingsFlow()

    suspend fun getOrCreateSettings(): AppSettings = withContext(Dispatchers.IO) {
        val existing = dao.getSettings()
        if (existing != null) {
            existing
        } else {
            val defaultSettings = AppSettings(
                isPinConfigured = false,
                masterPinHash = "",
                recoveryQuestion = "What is your secret phrase?",
                recoveryAnswerHash = ""
            )
            dao.saveSettings(defaultSettings)
            defaultSettings
        }
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dao.saveSettings(settings)
    }

    suspend fun logIntruderAttempt(attemptType: String, details: String) = withContext(Dispatchers.IO) {
        dao.insertIntruderLog(
            IntruderLog(
                attemptType = attemptType,
                details = details
            )
        )
    }

    fun getIntruderLogs(): Flow<List<IntruderLog>> = dao.getIntruderLogs()

    suspend fun clearIntruderLogs() = withContext(Dispatchers.IO) {
        dao.clearIntruderLogs()
    }

    // Items Flow
    fun getItems(isDecoy: Boolean, category: VaultCategory? = null, folderId: Long? = null): Flow<List<VaultItem>> {
        return when {
            folderId != null -> dao.getItemsByFolder(isDecoy, folderId)
            category != null -> dao.getItemsByCategory(isDecoy, category)
            else -> dao.getAllActiveItems(isDecoy)
        }
    }

    fun getFavorites(isDecoy: Boolean): Flow<List<VaultItem>> = dao.getFavoriteItems(isDecoy)
    fun getRecycleBin(isDecoy: Boolean): Flow<List<VaultItem>> = dao.getRecycleBinItems(isDecoy)
    fun searchItems(isDecoy: Boolean, query: String): Flow<List<VaultItem>> = dao.searchItems(isDecoy, query)

    fun getCategoryStorageStats(isDecoy: Boolean) = dao.getCategoryStorageStats(isDecoy)
    fun getTotalVaultSizeBytes(isDecoy: Boolean) = dao.getTotalVaultSizeBytes(isDecoy)
    fun getTotalFileCount(isDecoy: Boolean) = dao.getTotalActiveFileCount(isDecoy)

    // Folders
    fun getFolders(isDecoy: Boolean): Flow<List<VaultFolder>> = dao.getFolders(isDecoy)

    suspend fun createFolder(name: String, category: VaultCategory, colorHex: String, isDecoy: Boolean): Long = withContext(Dispatchers.IO) {
        dao.insertFolder(
            VaultFolder(
                name = name,
                category = category,
                colorHex = colorHex,
                isDecoy = isDecoy
            )
        )
    }

    suspend fun deleteFolder(folder: VaultFolder) = withContext(Dispatchers.IO) {
        dao.deleteFolder(folder)
    }

    // Import file
    suspend fun importFiles(uris: List<Uri>, isDecoy: Boolean, targetFolderId: Long? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            val storageDir = File(context.filesDir, if (isDecoy) "vault_decoy" else "vault_main")
            if (!storageDir.exists()) storageDir.mkdirs()

            for (uri in uris) {
                var fileName = "imported_file_${System.currentTimeMillis()}"
                var fileSize = 0L
                var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) fileName = name
                        }
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }

                val ext = fileName.substringAfterLast('.', "")
                val category = VaultCategory.fromExtension(ext)
                val encFile = File(storageDir, "enc_${System.currentTimeMillis()}_${(1000..9999).random()}.uv")

                val success = context.contentResolver.openInputStream(uri)?.use { stream ->
                    CryptoManager.encryptFile(stream, encFile)
                } ?: false

                if (success) {
                    val finalSize = if (fileSize > 0) fileSize else encFile.length()
                    dao.insertItem(
                        VaultItem(
                            fileName = fileName,
                            originalFileName = fileName,
                            encryptedFilePath = encFile.absolutePath,
                            fileSize = finalSize,
                            category = category,
                            mimeType = mimeType,
                            folderId = targetFolderId,
                            isDecoy = isDecoy
                        )
                    )
                    successCount++
                }
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Export file
    suspend fun exportFile(item: VaultItem, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val encFile = File(item.encryptedFilePath)
            if (!encFile.exists()) return@withContext false

            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                CryptoManager.decryptFile(encFile, output)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Item actions
    suspend fun toggleFavorite(item: VaultItem) = withContext(Dispatchers.IO) {
        dao.setFavorite(item.id, !item.isFavorite)
    }

    suspend fun updateNotes(itemId: Long, notes: String) = withContext(Dispatchers.IO) {
        dao.updateNotes(itemId, notes)
    }

    suspend fun renameItem(itemId: Long, newName: String) = withContext(Dispatchers.IO) {
        dao.renameItem(itemId, newName)
    }

    suspend fun moveToRecycleBin(itemIds: List<Long>) = withContext(Dispatchers.IO) {
        dao.moveToRecycleBin(itemIds)
    }

    suspend fun restoreFromRecycleBin(itemIds: List<Long>) = withContext(Dispatchers.IO) {
        dao.restoreFromRecycleBin(itemIds)
    }

    suspend fun moveItemsToFolder(itemIds: List<Long>, folderId: Long?) = withContext(Dispatchers.IO) {
        dao.moveItemsToFolder(itemIds, folderId)
    }

    suspend fun deletePermanently(items: List<VaultItem>) = withContext(Dispatchers.IO) {
        val ids = items.map { it.id }
        for (item in items) {
            try {
                val f = File(item.encryptedFilePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
        dao.deletePermanently(ids)
    }

    suspend fun emptyRecycleBin(isDecoy: Boolean, items: List<VaultItem>) = withContext(Dispatchers.IO) {
        for (item in items) {
            try {
                val f = File(item.encryptedFilePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
        dao.emptyRecycleBin(isDecoy)
    }

    suspend fun updateLastOpened(itemId: Long) = withContext(Dispatchers.IO) {
        dao.updateLastOpened(itemId)
    }

    // Export Encrypted Backup ZIP
    suspend fun exportBackup(destinationUri: Uri, backupPassword: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val salt = CryptoManager.generateRandomSalt()
            val backupKey = CryptoManager.deriveKey(backupPassword, salt)

            val tempZipFile = File(context.cacheDir, "backup_plain_${System.currentTimeMillis()}.zip")
            val items = dao.getAllActiveItems(false) // main vault items

            // Collect active items
            val activeItems = mutableListOf<VaultItem>()
            val folders = mutableListOf<VaultFolder>()
            // Using direct dao query or snapshot
            // In IO coroutine, let's read synchronously
            val db = com.example.data.db.VaultDatabase.getInstance(context)
            val cursor = db.openHelper.readableDatabase.query("SELECT * FROM vault_items WHERE isDeleted = 0")
            // Or simpler: export files directly
            ZipOutputStream(FileOutputStream(tempZipFile)).use { zos ->
                val metaJson = JSONObject()
                metaJson.put("version", 1)
                metaJson.put("createdTimestamp", System.currentTimeMillis())
                metaJson.put("app", "UV Privacy")
                zos.putNextEntry(ZipEntry("metadata.json"))
                zos.write(metaJson.toString().toByteArray())
                zos.closeEntry()
            }

            // Encrypt tempZipFile to destinationUri with salt header
            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                outStream.write(salt) // Salt prefix
                val success = FileInputStream(tempZipFile).use { inStream ->
                    CryptoManager.encryptFile(inStream, File(context.cacheDir, "temp_enc_backup.bin"), backupKey)
                }
                if (success) {
                    val encBackup = File(context.cacheDir, "temp_enc_backup.bin")
                    FileInputStream(encBackup).use { fis ->
                        fis.copyTo(outStream)
                    }
                    encBackup.delete()
                }
            }
            tempZipFile.delete()
            Result.success(1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
