package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.auth.CreateMasterPinDialog
import com.example.ui.auth.RecoveryDialog
import com.example.ui.auth.SecretPinDialog
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.clock.ClockScreen
import com.example.ui.flashlight.FlashlightController
import com.example.ui.flashlight.FlashlightScreen
import com.example.ui.theme.UVPrivacyTheme
import com.example.ui.vault.AboutCreatorAndGuideDialog
import com.example.ui.vault.VaultMainScreen
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VaultViewModel by viewModels()
    private lateinit var flashlightController: FlashlightController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        flashlightController = FlashlightController(applicationContext)

        // Lock vault automatically if app goes to background
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                flashlightController.turnOff()
                viewModel.onAppBackgrounded()
            }
        })

        setContent {
            val settings by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
            val isDecoyVault by viewModel.isDecoyVault.collectAsState()
            val showAuthDialog by viewModel.showAuthDialog.collectAsState()
            val showCreatePinDialog by viewModel.showCreatePinDialog.collectAsState()
            val showRecoveryDialog by viewModel.showRecoveryDialog.collectAsState()
            val showAboutGuideDialog by viewModel.showAboutGuideDialog.collectAsState()
            val authError by viewModel.authError.collectAsState()

            val items by viewModel.items.collectAsState()
            val folders by viewModel.folders.collectAsState()
            val storageStats by viewModel.storageStats.collectAsState()
            val totalSizeBytes by viewModel.totalSizeBytes.collectAsState()
            val totalFiles by viewModel.totalFileCount.collectAsState()
            val intruderLogs by viewModel.intruderLogs.collectAsState()
            val recycleBinItems by viewModel.recycleBinItems.collectAsState()

            val batteryLevel by flashlightController.batteryLevel.collectAsState()
            val isCharging by flashlightController.isCharging.collectAsState()

            // Keep screen on handler
            LaunchedEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            UVPrivacyTheme(themeStyle = "AMOLED") {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            AppScreen.CLOCK -> {
                                ClockScreen(
                                    settings = settings,
                                    batteryLevel = batteryLevel,
                                    isCharging = isCharging,
                                    onOpenFlashlight = { viewModel.navigateTo(AppScreen.FLASHLIGHT) },
                                    onOpenCalculator = { viewModel.navigateTo(AppScreen.CALCULATOR) },
                                    onTriggerSecretEntry = { viewModel.showPinPrompt() },
                                    onOpenAboutGuide = { viewModel.showAboutGuide() },
                                    onUpdateSettings = { viewModel.updateSettings(it) }
                                )
                            }

                            AppScreen.FLASHLIGHT -> {
                                BackHandler {
                                    flashlightController.turnOff()
                                    viewModel.navigateTo(AppScreen.CLOCK)
                                }
                                FlashlightScreen(
                                    controller = flashlightController,
                                    onBackToClock = {
                                        flashlightController.turnOff()
                                        viewModel.navigateTo(AppScreen.CLOCK)
                                    }
                                )
                            }

                            AppScreen.CALCULATOR -> {
                                BackHandler {
                                    viewModel.navigateTo(AppScreen.CLOCK)
                                }
                                CalculatorScreen(
                                    onEvaluateSecret = { candidate ->
                                        viewModel.evaluateCalculatorSecret(candidate)
                                    },
                                    onBackToClock = { viewModel.navigateTo(AppScreen.CLOCK) }
                                )
                            }

                            AppScreen.VAULT -> {
                                VaultMainScreen(
                                    isDecoy = isDecoyVault,
                                    items = items,
                                    folders = folders,
                                    storageStats = storageStats,
                                    totalSizeBytes = totalSizeBytes,
                                    totalFiles = totalFiles,
                                    intruderLogs = intruderLogs,
                                    recycleBinItems = recycleBinItems,
                                    settings = settings,
                                    onLockVault = { viewModel.lockVault() },
                                    onImportFiles = { uris, folderId -> viewModel.importFiles(uris, folderId) },
                                    onExportFile = { item, uri -> viewModel.exportFile(item, uri) },
                                    onDeleteItems = { ids -> viewModel.deleteItems(ids) },
                                    onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                                    onRenameItem = { id, name -> viewModel.renameItem(id, name) },
                                    onUpdateNotes = { id, notes -> viewModel.updateNotes(id, notes) },
                                    onMoveItemsToFolder = { ids, fId -> viewModel.moveItemsToFolder(ids, fId) },
                                    onCreateFolder = { name, cat, color -> viewModel.createFolder(name, cat, color) },
                                    onDeleteFolder = { folder -> viewModel.deleteFolder(folder) },
                                    onRestoreRecycleItems = { ids -> viewModel.restoreRecycleItems(ids) },
                                    onDeletePermanently = { delItems -> viewModel.deletePermanently(delItems) },
                                    onEmptyRecycleBin = { viewModel.emptyRecycleBin() },
                                    onClearIntruderLogs = { viewModel.clearIntruderLogs() },
                                    onUpdateSettings = { viewModel.updateSettings(it) },
                                    onExportBackup = { uri, pwd -> viewModel.exportBackup(uri, pwd) }
                                )
                            }
                        }
                    }

                    // Secret PIN Verification Dialog
                    if (showAuthDialog) {
                        SecretPinDialog(
                            onDismiss = { viewModel.dismissPinPrompt() },
                            onSubmitPin = { pin -> viewModel.verifyAndUnlock(pin) },
                            onForgotPin = { viewModel.showRecoveryPrompt() },
                            onSetupNewPin = { viewModel.showCreatePinPrompt() },
                            errorMessage = authError
                        )
                    }

                    // Create / Setup Master PIN Dialog
                    if (showCreatePinDialog) {
                        CreateMasterPinDialog(
                            onDismiss = { viewModel.dismissCreatePinPrompt() },
                            onPinCreated = { pin, question, answer ->
                                viewModel.createMasterPin(pin, question, answer)
                            }
                        )
                    }

                    // Security Recovery Dialog
                    if (showRecoveryDialog) {
                        RecoveryDialog(
                            question = settings.recoveryQuestion,
                            onDismiss = { viewModel.dismissRecoveryPrompt() },
                            onSubmitAnswer = { answer -> viewModel.verifyRecoveryAnswer(answer) }
                        )
                    }

                    // About Creator & Secret Guide Dialog
                    if (showAboutGuideDialog) {
                        AboutCreatorAndGuideDialog(
                            onDismiss = { viewModel.dismissAboutGuide() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        flashlightController.destroy()
    }
}
