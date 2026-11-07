package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.crypto.CryptoManager
import com.example.data.db.CategoryStorageStat
import com.example.data.db.VaultDatabase
import com.example.data.model.*
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    CLOCK,
    FLASHLIGHT,
    CALCULATOR,
    VAULT
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VaultDatabase.getInstance(application)
    private val repository = VaultRepository(application, database.vaultDao())

    private val _currentScreen = MutableStateFlow(AppScreen.CLOCK)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _isDecoyVault = MutableStateFlow(false)
    val isDecoyVault: StateFlow<Boolean> = _isDecoyVault.asStateFlow()

    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog: StateFlow<Boolean> = _showAuthDialog.asStateFlow()

    private val _showCreatePinDialog = MutableStateFlow(false)
    val showCreatePinDialog: StateFlow<Boolean> = _showCreatePinDialog.asStateFlow()

    private val _showRecoveryDialog = MutableStateFlow(false)
    val showRecoveryDialog: StateFlow<Boolean> = _showRecoveryDialog.asStateFlow()

    private val _showAboutGuideDialog = MutableStateFlow(false)
    val showAboutGuideDialog: StateFlow<Boolean> = _showAboutGuideDialog.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var failedAttempts = 0

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    val items: StateFlow<List<VaultItem>> = _isDecoyVault
        .flatMapLatest { decoy -> repository.getItems(decoy) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val folders: StateFlow<List<VaultFolder>> = _isDecoyVault
        .flatMapLatest { decoy -> repository.getFolders(decoy) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recycleBinItems: StateFlow<List<VaultItem>> = _isDecoyVault
        .flatMapLatest { decoy -> repository.getRecycleBin(decoy) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val storageStats: StateFlow<List<CategoryStorageStat>> = _isDecoyVault
        .flatMapLatest { decoy -> repository.getCategoryStorageStats(decoy) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalSizeBytes: StateFlow<Long> = _isDecoyVault
        .flatMapLatest { decoy -> repository.getTotalVaultSizeBytes(decoy) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalFileCount: StateFlow<Int> = _isDecoyVault
        .flatMapLatest { decoy -> repository.getTotalFileCount(decoy) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val intruderLogs: StateFlow<List<IntruderLog>> = repository.getIntruderLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            repository.getOrCreateSettings()
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.VAULT && !_isVaultUnlocked.value) {
            showPinPrompt()
        } else {
            _currentScreen.value = screen
        }
    }

    fun showPinPrompt() {
        _authError.value = null
        if (!settings.value.isPinConfigured || settings.value.masterPinHash.isEmpty()) {
            _showCreatePinDialog.value = true
        } else {
            _showAuthDialog.value = true
        }
    }

    fun dismissPinPrompt() {
        _showAuthDialog.value = false
        _authError.value = null
    }

    fun showCreatePinPrompt() {
        _showAuthDialog.value = false
        _showCreatePinDialog.value = true
    }

    fun dismissCreatePinPrompt() {
        _showCreatePinDialog.value = false
    }

    fun showRecoveryPrompt() {
        _showAuthDialog.value = false
        _showCreatePinDialog.value = false
        _showRecoveryDialog.value = true
    }

    fun dismissRecoveryPrompt() {
        _showRecoveryDialog.value = false
    }

    fun showAboutGuide() {
        _showAboutGuideDialog.value = true
    }

    fun dismissAboutGuide() {
        _showAboutGuideDialog.value = false
    }

    fun createMasterPin(pin: String, question: String, answer: String) {
        viewModelScope.launch {
            val salt = CryptoManager.generateRandomSalt()
            val masterHash = CryptoManager.hashPin(pin, salt)
            val ansHash = CryptoManager.hashPin(answer.lowercase().trim(), salt)
            val updated = settings.value.copy(
                isPinConfigured = true,
                masterPinHash = masterHash,
                recoveryQuestion = question,
                recoveryAnswerHash = ansHash
            )
            repository.saveSettings(updated)
            CryptoManager.startSession(pin, masterHash)
            _isVaultUnlocked.value = true
            _isDecoyVault.value = false
            _showCreatePinDialog.value = false
            _showAuthDialog.value = false
            _currentScreen.value = AppScreen.VAULT
            Toast.makeText(getApplication(), "Master PIN Created! Vault Unlocked.", Toast.LENGTH_SHORT).show()
        }
    }

    fun verifyAndUnlock(pin: String): Boolean {
        val currentSettings = settings.value

        // Check if PIN has not been set yet
        if (!currentSettings.isPinConfigured || currentSettings.masterPinHash.isEmpty()) {
            _showAuthDialog.value = false
            _showCreatePinDialog.value = true
            return false
        }

        // Check Master PIN
        if (CryptoManager.verifyPin(pin, currentSettings.masterPinHash)) {
            CryptoManager.startSession(pin, currentSettings.masterPinHash)
            _isVaultUnlocked.value = true
            _isDecoyVault.value = false
            _showAuthDialog.value = false
            _authError.value = null
            failedAttempts = 0
            _currentScreen.value = AppScreen.VAULT
            return true
        }

        // Check Decoy PIN
        if (currentSettings.isDecoyEnabled && currentSettings.decoyPinHash.isNotEmpty() &&
            CryptoManager.verifyPin(pin, currentSettings.decoyPinHash)
        ) {
            CryptoManager.startSession(pin, currentSettings.decoyPinHash)
            _isVaultUnlocked.value = true
            _isDecoyVault.value = true
            _showAuthDialog.value = false
            _authError.value = null
            failedAttempts = 0
            _currentScreen.value = AppScreen.VAULT
            return true
        }

        // Failed attempt
        failedAttempts++
        viewModelScope.launch {
            repository.logIntruderAttempt(
                attemptType = "Incorrect PIN",
                details = "Attempt #$failedAttempts with ${pin.length} digits"
            )
        }
        _authError.value = "Incorrect PIN ($failedAttempts failed attempts)"
        return false
    }

    fun evaluateCalculatorSecret(candidate: String): Boolean {
        if (!settings.value.isPinConfigured || settings.value.masterPinHash.isEmpty()) {
            if (candidate.length in 4..8 && candidate.all { it.isDigit() }) {
                _showCreatePinDialog.value = true
                return true
            }
            return false
        }
        if (candidate.length in 4..8 && candidate.all { it.isDigit() }) {
            return verifyAndUnlock(candidate)
        }
        return false
    }

    fun verifyRecoveryAnswer(answer: String) {
        val currentSettings = settings.value
        if (CryptoManager.verifyPin(answer.lowercase().trim(), currentSettings.recoveryAnswerHash)) {
            _showRecoveryDialog.value = false
            _showCreatePinDialog.value = true
            Toast.makeText(getApplication(), "Answer verified! Set your new Master PIN.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(getApplication(), "Incorrect recovery answer", Toast.LENGTH_SHORT).show()
        }
    }

    fun lockVault() {
        CryptoManager.clearSession(getApplication())
        _isVaultUnlocked.value = false
        _showAuthDialog.value = false
        _showCreatePinDialog.value = false
        _currentScreen.value = AppScreen.CLOCK
    }

    fun onAppBackgrounded() {
        if (settings.value.lockOnBackground && _isVaultUnlocked.value) {
            lockVault()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun importFiles(uris: List<Uri>, folderId: Long? = null) {
        viewModelScope.launch {
            val result = repository.importFiles(uris, _isDecoyVault.value, folderId)
            result.onSuccess { count ->
                Toast.makeText(getApplication(), "Encrypted & secured $count items", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(getApplication(), "Import error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportFile(item: VaultItem, destinationUri: Uri) {
        viewModelScope.launch {
            val success = repository.exportFile(item, destinationUri)
            if (success) {
                Toast.makeText(getApplication(), "File decrypted & exported", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "Failed to export file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleFavorite(item: VaultItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun updateNotes(itemId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(itemId, notes)
        }
    }

    fun renameItem(itemId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameItem(itemId, newName)
        }
    }

    fun deleteItems(itemIds: List<Long>) {
        viewModelScope.launch {
            repository.moveToRecycleBin(itemIds)
            Toast.makeText(getApplication(), "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreRecycleItems(itemIds: List<Long>) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(itemIds)
            Toast.makeText(getApplication(), "Items restored", Toast.LENGTH_SHORT).show()
        }
    }

    fun deletePermanently(items: List<VaultItem>) {
        viewModelScope.launch {
            repository.deletePermanently(items)
            Toast.makeText(getApplication(), "Permanently shredded", Toast.LENGTH_SHORT).show()
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin(_isDecoyVault.value, recycleBinItems.value)
            Toast.makeText(getApplication(), "Recycle bin emptied", Toast.LENGTH_SHORT).show()
        }
    }

    fun moveItemsToFolder(itemIds: List<Long>, folderId: Long?) {
        viewModelScope.launch {
            repository.moveItemsToFolder(itemIds, folderId)
        }
    }

    fun createFolder(name: String, category: VaultCategory, colorHex: String) {
        viewModelScope.launch {
            repository.createFolder(name, category, colorHex, _isDecoyVault.value)
        }
    }

    fun deleteFolder(folder: VaultFolder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    fun clearIntruderLogs() {
        viewModelScope.launch {
            repository.clearIntruderLogs()
        }
    }

    fun exportBackup(destinationUri: Uri, backupPassword: String) {
        viewModelScope.launch {
            val result = repository.exportBackup(destinationUri, backupPassword)
            result.onSuccess {
                Toast.makeText(getApplication(), "Backup encrypted and exported successfully", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(getApplication(), "Backup export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
