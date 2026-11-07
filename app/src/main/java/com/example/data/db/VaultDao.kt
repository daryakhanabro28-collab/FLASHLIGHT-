package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

data class CategoryStorageStat(
    val category: VaultCategory,
    val count: Int,
    val totalBytes: Long
)

@Dao
interface VaultDao {
    // Items
    @Query("SELECT * FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0 ORDER BY importedTimestamp DESC")
    fun getAllActiveItems(isDecoy: Boolean): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0 AND category = :category ORDER BY importedTimestamp DESC")
    fun getItemsByCategory(isDecoy: Boolean, category: VaultCategory): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0 AND folderId = :folderId ORDER BY importedTimestamp DESC")
    fun getItemsByFolder(isDecoy: Boolean, folderId: Long): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0 AND isFavorite = 1 ORDER BY importedTimestamp DESC")
    fun getFavoriteItems(isDecoy: Boolean): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getRecycleBinItems(isDecoy: Boolean): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): VaultItem?

    @Query("SELECT * FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0 AND (fileName LIKE '%' || :query || '%' OR originalFileName LIKE '%' || :query || '%')")
    fun searchItems(isDecoy: Boolean, query: String): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<VaultItem>): List<Long>

    @Update
    suspend fun updateItem(item: VaultItem)

    @Query("UPDATE vault_items SET isDeleted = 1, deletedTimestamp = :timestamp WHERE id IN (:itemIds)")
    suspend fun moveToRecycleBin(itemIds: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_items SET isDeleted = 0, deletedTimestamp = NULL WHERE id IN (:itemIds)")
    suspend fun restoreFromRecycleBin(itemIds: List<Long>)

    @Query("DELETE FROM vault_items WHERE id IN (:itemIds)")
    suspend fun deletePermanently(itemIds: List<Long>)

    @Query("DELETE FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 1")
    suspend fun emptyRecycleBin(isDecoy: Boolean)

    @Query("UPDATE vault_items SET folderId = :folderId WHERE id IN (:itemIds)")
    suspend fun moveItemsToFolder(itemIds: List<Long>, folderId: Long?)

    @Query("UPDATE vault_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE vault_items SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Query("UPDATE vault_items SET fileName = :newName WHERE id = :id")
    suspend fun renameItem(id: Long, newName: String)

    @Query("UPDATE vault_items SET lastOpenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT category, COUNT(id) as count, COALESCE(SUM(fileSize), 0) as totalBytes FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0 GROUP BY category")
    fun getCategoryStorageStats(isDecoy: Boolean): Flow<List<CategoryStorageStat>>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0")
    fun getTotalVaultSizeBytes(isDecoy: Boolean): Flow<Long>

    @Query("SELECT COUNT(id) FROM vault_items WHERE isDecoy = :isDecoy AND isDeleted = 0")
    fun getTotalActiveFileCount(isDecoy: Boolean): Flow<Int>

    // Folders
    @Query("SELECT * FROM vault_folders WHERE isDecoy = :isDecoy ORDER BY name ASC")
    fun getFolders(isDecoy: Boolean): Flow<List<VaultFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: VaultFolder): Long

    @Delete
    suspend fun deleteFolder(folder: VaultFolder)

    // Intruder Logs
    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC LIMIT 50")
    fun getIntruderLogs(): Flow<List<IntruderLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderLog(log: IntruderLog)

    @Query("DELETE FROM intruder_logs")
    suspend fun clearIntruderLogs()

    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}
