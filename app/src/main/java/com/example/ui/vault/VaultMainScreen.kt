package com.example.ui.vault

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.db.CategoryStorageStat
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class VaultNavTab {
    FILES,
    FOLDERS,
    SECURITY,
    SETTINGS
}

enum class SortField {
    DATE,
    NAME,
    SIZE,
    TYPE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    isDecoy: Boolean,
    items: List<VaultItem>,
    folders: List<VaultFolder>,
    storageStats: List<CategoryStorageStat>,
    totalSizeBytes: Long,
    totalFiles: Int,
    intruderLogs: List<IntruderLog>,
    recycleBinItems: List<VaultItem>,
    settings: AppSettings,
    onLockVault: () -> Unit,
    onImportFiles: (List<Uri>, Long?) -> Unit,
    onExportFile: (VaultItem, Uri) -> Unit,
    onDeleteItems: (List<Long>) -> Unit,
    onToggleFavorite: (VaultItem) -> Unit,
    onRenameItem: (Long, String) -> Unit,
    onUpdateNotes: (Long, String) -> Unit,
    onMoveItemsToFolder: (List<Long>, Long?) -> Unit,
    onCreateFolder: (String, VaultCategory, String) -> Unit,
    onDeleteFolder: (VaultFolder) -> Unit,
    onRestoreRecycleItems: (List<Long>) -> Unit,
    onDeletePermanently: (List<VaultItem>) -> Unit,
    onEmptyRecycleBin: () -> Unit,
    onClearIntruderLogs: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onExportBackup: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(VaultNavTab.FILES) }
    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var sortField by remember { mutableStateOf(SortField.DATE) }
    var sortAscending by remember { mutableStateOf(false) }

    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedItemIds by remember { mutableStateOf(setOf<Long>()) }

    // Modals & Viewers
    var activePhotoItem by remember { mutableStateOf<VaultItem?>(null) }
    var activeVideoItem by remember { mutableStateOf<VaultItem?>(null) }
    var activeDocItem by remember { mutableStateOf<VaultItem?>(null) }
    var activeDetailItem by remember { mutableStateOf<VaultItem?>(null) }
    var showRecycleBinScreen by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showImportSpeedDial by remember { mutableStateOf(false) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }

    // Item to export with SAF CreateDocument launcher
    var pendingExportItem by remember { mutableStateOf<VaultItem?>(null) }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null && pendingExportItem != null) {
            onExportFile(pendingExportItem!!, uri)
            pendingExportItem = null
        }
    }

    // Backup archive export launcher
    var pendingBackupPassword by remember { mutableStateOf("") }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && pendingBackupPassword.isNotEmpty()) {
            onExportBackup(uri, pendingBackupPassword)
            pendingBackupPassword = ""
        }
    }

    // Media Picker (Photos & Videos)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportFiles(uris, selectedFolderId)
        }
    }

    // Generic Document/File Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportFiles(uris, selectedFolderId)
        }
    }

    BackHandler {
        if (isMultiSelectMode) {
            isMultiSelectMode = false
            selectedItemIds = emptySet()
        } else if (showRecycleBinScreen) {
            showRecycleBinScreen = false
        } else if (selectedFolderId != null) {
            selectedFolderId = null
        } else {
            onLockVault()
        }
    }

    if (showRecycleBinScreen) {
        RecycleBinScreen(
            items = recycleBinItems,
            onRestoreItems = onRestoreRecycleItems,
            onDeletePermanently = onDeletePermanently,
            onEmptyRecycleBin = onEmptyRecycleBin,
            onBack = { showRecycleBinScreen = false }
        )
        return
    }

    // Filter and Sort Items
    val filteredItems = remember(items, selectedCategory, selectedFolderId, searchQuery, sortField, sortAscending) {
        var list = items

        if (selectedFolderId != null) {
            list = list.filter { it.folderId == selectedFolderId }
        } else if (selectedCategory != null) {
            list = list.filter { it.category == selectedCategory }
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.fileName.contains(searchQuery, ignoreCase = true) ||
                        it.originalFileName.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortField) {
            SortField.DATE -> if (sortAscending) list.sortedBy { it.importedTimestamp } else list.sortedByDescending { it.importedTimestamp }
            SortField.NAME -> if (sortAscending) list.sortedBy { it.fileName.lowercase() } else list.sortedByDescending { it.fileName.lowercase() }
            SortField.SIZE -> if (sortAscending) list.sortedBy { it.fileSize } else list.sortedByDescending { it.fileSize }
            SortField.TYPE -> if (sortAscending) list.sortedBy { it.category.name } else list.sortedByDescending { it.category.name }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("vault_main_scaffold"),
        containerColor = Color(0xFF09090B),
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF09090B)).statusBarsPadding()) {
                if (isMultiSelectMode) {
                    // Multi-select top action bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                isMultiSelectMode = false
                                selectedItemIds = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                            Text("${selectedItemIds.size} Selected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = {
                                selectedItemIds = if (selectedItemIds.size == filteredItems.size) emptySet() else filteredItems.map { it.id }.toSet()
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.LightGray)
                            }
                            IconButton(onClick = { showMoveFolderDialog = true }) {
                                Icon(Icons.Default.DriveFileMove, contentDescription = "Move to Folder", tint = Color(0xFF00E5FF))
                            }
                            IconButton(onClick = {
                                onDeleteItems(selectedItemIds.toList())
                                isMultiSelectMode = false
                                selectedItemIds = emptySet()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                            }
                        }
                    }
                } else {
                    // Standard Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDecoy) Color(0xFFFF9800).copy(alpha = 0.2f) else Color(0xFF00E5FF).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDecoy) Icons.Default.ShieldMoon else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isDecoy) Color(0xFFFF9800) else Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isDecoy) "DECOY VAULT" else "PRIVATE VAULT",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${items.size} files • ${formatFileSize(totalSizeBytes)}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Recycle Bin badge
                            IconButton(onClick = { showRecycleBinScreen = true }) {
                                BadgedBox(
                                    badge = {
                                        if (recycleBinItems.isNotEmpty()) {
                                            Badge(containerColor = Color(0xFFFF5252)) {
                                                Text("${recycleBinItems.size}", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Recycle Bin", tint = Color.LightGray)
                                }
                            }

                            // Lock Vault Button
                            IconButton(
                                onClick = onLockVault,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1E26))
                                    .testTag("lock_vault_button")
                            ) {
                                Icon(Icons.Default.LockClock, contentDescription = "Lock Vault", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Search & View Controls (Only in Files/Folders tabs)
                    if (currentTab == VaultNavTab.FILES || currentTab == VaultNavTab.FOLDERS) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search files...", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF141418),
                                    unfocusedContainerColor = Color(0xFF141418),
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF22222A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            )

                            IconButton(
                                onClick = { isGridView = !isGridView },
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF141418))
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle Grid/List",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Category Pills (Only when on Files tab)
                        if (currentTab == VaultNavTab.FILES) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null && selectedFolderId == null,
                                    onClick = {
                                        selectedCategory = null
                                        selectedFolderId = null
                                    },
                                    label = { Text("All", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = selectedCategory == VaultCategory.PHOTO,
                                    onClick = { selectedCategory = if (selectedCategory == VaultCategory.PHOTO) null else VaultCategory.PHOTO },
                                    label = { Text("Photos", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = selectedCategory == VaultCategory.VIDEO,
                                    onClick = { selectedCategory = if (selectedCategory == VaultCategory.VIDEO) null else VaultCategory.VIDEO },
                                    label = { Text("Videos", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = selectedCategory == VaultCategory.DOCUMENT,
                                    onClick = { selectedCategory = if (selectedCategory == VaultCategory.DOCUMENT) null else VaultCategory.DOCUMENT },
                                    label = { Text("Docs", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = selectedCategory == VaultCategory.OTHER,
                                    onClick = { selectedCategory = if (selectedCategory == VaultCategory.OTHER) null else VaultCategory.OTHER },
                                    label = { Text("Other", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F0F13),
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("vault_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentTab == VaultNavTab.FILES,
                    onClick = { currentTab = VaultNavTab.FILES },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                    label = { Text("Files", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00E5FF),
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00363D)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == VaultNavTab.FOLDERS,
                    onClick = { currentTab = VaultNavTab.FOLDERS },
                    icon = { Icon(Icons.Default.Collections, contentDescription = "Albums") },
                    label = { Text("Albums", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00E5FF),
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00363D)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == VaultNavTab.SECURITY,
                    onClick = { currentTab = VaultNavTab.SECURITY },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
                    label = { Text("Security", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00E5FF),
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00363D)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == VaultNavTab.SETTINGS,
                    onClick = { currentTab = VaultNavTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00E5FF),
                        selectedTextColor = Color(0xFF00E5FF),
                        indicatorColor = Color(0xFF00363D)
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab == VaultNavTab.FILES || currentTab == VaultNavTab.FOLDERS) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showImportSpeedDial) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showImportSpeedDial = false
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            icon = { Icon(Icons.Default.Image, contentDescription = null) },
                            text = { Text("Import Photos & Videos", fontSize = 12.sp) },
                            containerColor = Color(0xFF1E1E26),
                            contentColor = Color(0xFF00E5FF),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ExtendedFloatingActionButton(
                            onClick = {
                                showImportSpeedDial = false
                                docPickerLauncher.launch(arrayOf("*/*"))
                            },
                            icon = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                            text = { Text("Import Files & Docs", fontSize = 12.sp) },
                            containerColor = Color(0xFF1E1E26),
                            contentColor = Color(0xFFFFB300),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ExtendedFloatingActionButton(
                            onClick = {
                                showImportSpeedDial = false
                                showCreateFolderDialog = true
                            },
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            text = { Text("New Folder / Album", fontSize = 12.sp) },
                            containerColor = Color(0xFF1E1E26),
                            contentColor = Color(0xFF00E676),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { showImportSpeedDial = !showImportSpeedDial },
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.testTag("vault_import_fab")
                    ) {
                        Icon(
                            imageVector = if (showImportSpeedDial) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Import Files"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                VaultNavTab.FILES -> {
                    if (filteredItems.isEmpty()) {
                        EmptyVaultState(
                            hasSearch = searchQuery.isNotBlank(),
                            onImportMedia = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                        )
                    } else if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                VaultItemGridCell(
                                    item = item,
                                    isMultiSelect = isMultiSelectMode,
                                    isSelected = item.id in selectedItemIds,
                                    onSelect = {
                                        selectedItemIds = if (item.id in selectedItemIds) selectedItemIds - item.id else selectedItemIds + item.id
                                    },
                                    onLongPress = {
                                        isMultiSelectMode = true
                                        selectedItemIds = setOf(item.id)
                                    },
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            selectedItemIds = if (item.id in selectedItemIds) selectedItemIds - item.id else selectedItemIds + item.id
                                        } else {
                                            when (item.category) {
                                                VaultCategory.PHOTO -> activePhotoItem = item
                                                VaultCategory.VIDEO -> activeVideoItem = item
                                                VaultCategory.DOCUMENT -> activeDocItem = item
                                                VaultCategory.OTHER -> activeDetailItem = item
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                VaultItemListRow(
                                    item = item,
                                    isMultiSelect = isMultiSelectMode,
                                    isSelected = item.id in selectedItemIds,
                                    onSelect = {
                                        selectedItemIds = if (item.id in selectedItemIds) selectedItemIds - item.id else selectedItemIds + item.id
                                    },
                                    onLongPress = {
                                        isMultiSelectMode = true
                                        selectedItemIds = setOf(item.id)
                                    },
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            selectedItemIds = if (item.id in selectedItemIds) selectedItemIds - item.id else selectedItemIds + item.id
                                        } else {
                                            when (item.category) {
                                                VaultCategory.PHOTO -> activePhotoItem = item
                                                VaultCategory.VIDEO -> activeVideoItem = item
                                                VaultCategory.DOCUMENT -> activeDocItem = item
                                                VaultCategory.OTHER -> activeDetailItem = item
                                            }
                                        }
                                    },
                                    onToggleFavorite = { onToggleFavorite(item) },
                                    onDetails = { activeDetailItem = item },
                                    onExport = {
                                        pendingExportItem = item
                                        exportFileLauncher.launch(item.fileName)
                                    },
                                    onDelete = { onDeleteItems(listOf(item.id)) }
                                )
                            }
                        }
                    }
                }

                VaultNavTab.FOLDERS -> {
                    VaultAlbumsScreen(
                        folders = folders,
                        items = items,
                        onSelectFolder = { folderId ->
                            selectedFolderId = folderId
                            currentTab = VaultNavTab.FILES
                        },
                        onCreateFolder = { showCreateFolderDialog = true },
                        onDeleteFolder = onDeleteFolder
                    )
                }

                VaultNavTab.SECURITY -> {
                    SecurityDashboardScreen(
                        settings = settings,
                        storageStats = storageStats,
                        totalSizeBytes = totalSizeBytes,
                        totalFiles = totalFiles,
                        intruderLogs = intruderLogs,
                        onClearIntruderLogs = onClearIntruderLogs
                    )
                }

                VaultNavTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onUpdateSettings = onUpdateSettings,
                        onOpenBackup = { showBackupDialog = true },
                        onOpenRecycleBin = { showRecycleBinScreen = true }
                    )
                }
            }
        }
    }

    // Viewers & Modals
    activePhotoItem?.let { item ->
        FullscreenPhotoViewer(
            item = item,
            onClose = { activePhotoItem = null },
            onToggleFavorite = { onToggleFavorite(item) },
            onExport = {
                pendingExportItem = item
                exportFileLauncher.launch(item.fileName)
            },
            onDelete = {
                onDeleteItems(listOf(item.id))
                activePhotoItem = null
            }
        )
    }

    activeVideoItem?.let { item ->
        FullscreenVideoPlayer(
            item = item,
            onClose = { activeVideoItem = null },
            onExport = {
                pendingExportItem = item
                exportFileLauncher.launch(item.fileName)
            }
        )
    }

    activeDocItem?.let { item ->
        DocumentTextViewer(
            item = item,
            onClose = { activeDocItem = null },
            onExport = {
                pendingExportItem = item
                exportFileLauncher.launch(item.fileName)
            }
        )
    }

    activeDetailItem?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { activeDetailItem = null },
            onSaveNotes = { notes -> onUpdateNotes(item.id, notes) },
            onRename = { newName -> onRenameItem(item.id, newName) }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, cat, color ->
                onCreateFolder(name, cat, color)
                showCreateFolderDialog = false
            }
        )
    }

    if (showBackupDialog) {
        BackupPasswordDialog(
            isExport = true,
            onDismiss = { showBackupDialog = false },
            onSubmitPassword = { pwd ->
                pendingBackupPassword = pwd
                showBackupDialog = false
                exportBackupLauncher.launch("UV_Privacy_Backup_${System.currentTimeMillis()}.bin")
            }
        )
    }

    if (showMoveFolderDialog) {
        MoveToFolderDialog(
            folders = folders,
            onDismiss = { showMoveFolderDialog = false },
            onSelectFolder = { folderId ->
                onMoveItemsToFolder(selectedItemIds.toList(), folderId)
                showMoveFolderDialog = false
                isMultiSelectMode = false
                selectedItemIds = emptySet()
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VaultItemGridCell(
    item: VaultItem,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    var thumbnailBitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }

    if (item.category == VaultCategory.PHOTO) {
        LaunchedEffect(item.id) {
            withContext(Dispatchers.IO) {
                thumbnailBitmap = com.example.data.crypto.CryptoManager.decryptThumbnailBitmap(File(item.encryptedFilePath))
            }
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141418))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnailBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = item.fileName,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Bottom gradient for filename readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.fileName,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (item.category) {
                            VaultCategory.PHOTO -> Icons.Default.Image
                            VaultCategory.VIDEO -> Icons.Default.Videocam
                            VaultCategory.DOCUMENT -> Icons.Default.Description
                            VaultCategory.OTHER -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = when (item.category) {
                            VaultCategory.PHOTO -> Color(0xFF00E5FF)
                            VaultCategory.VIDEO -> Color(0xFF7C4DFF)
                            VaultCategory.DOCUMENT -> Color(0xFFFFB300)
                            VaultCategory.OTHER -> Color(0xFF00E676)
                        },
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.fileName,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formatFileSize(item.fileSize),
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }

            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                )
            } else if (item.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = Color(0xFFFF4081),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VaultItemListRow(
    item: VaultItem,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDetails: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1E1E2C) else Color(0xFF141418))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C1C24)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.category) {
                        VaultCategory.PHOTO -> Icons.Default.Image
                        VaultCategory.VIDEO -> Icons.Default.Videocam
                        VaultCategory.DOCUMENT -> Icons.Default.Description
                        VaultCategory.OTHER -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = when (item.category) {
                        VaultCategory.PHOTO -> Color(0xFF00E5FF)
                        VaultCategory.VIDEO -> Color(0xFF7C4DFF)
                        VaultCategory.DOCUMENT -> Color(0xFFFFB300)
                        VaultCategory.OTHER -> Color(0xFF00E676)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.fileName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text("${formatFileSize(item.fileSize)} • AES-256-GCM", color = Color.Gray, fontSize = 11.sp)
            }

            if (!isMultiSelect) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFFF4081) else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = Color(0xFF1E1E26)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Details & Notes", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onDetails()
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00E5FF)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Decrypted", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.LightGray) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete (Recycle Bin)", color = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultAlbumsScreen(
    folders: List<VaultFolder>,
    items: List<VaultItem>,
    onSelectFolder: (Long) -> Unit,
    onCreateFolder: () -> Unit,
    onDeleteFolder: (VaultFolder) -> Unit
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No custom albums created yet", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCreateFolder,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Create First Album", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders, key = { it.id }) { folder ->
                val count = items.count { it.folderId == folder.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFolder(folder.id) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(folder.colorHex)).copy(alpha = 0.2f)
                                        } catch (e: Exception) {
                                            Color(0xFF00E5FF).copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { onDeleteFolder(folder) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Album", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(folder.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("$count items", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyVaultState(
    hasSearch: Boolean,
    onImportMedia: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = if (hasSearch) Icons.Default.SearchOff else Icons.Default.Lock,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (hasSearch) "No matching files found" else "Vault is Empty",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasSearch) "Try refining your search terms" else "Import photos, videos, or documents to encrypt them with AES-256.",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!hasSearch) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onImportMedia,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Files Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, VaultCategory, String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#00E5FF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Album / Folder", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Album Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onCreate(folderName.trim(), VaultCategory.PHOTO, selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun MoveToFolderDialog(
    folders: List<VaultFolder>,
    onDismiss: () -> Unit,
    onSelectFolder: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Album", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onSelectFolder(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vault Root (No Album)", color = Color(0xFF00E5FF), modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = Color(0xFF222228))
                folders.forEach { f ->
                    TextButton(
                        onClick = { onSelectFolder(f.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(f.name, color = Color.White, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF16161A)
    )
}
