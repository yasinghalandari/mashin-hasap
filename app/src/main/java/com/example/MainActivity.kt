package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserPreferences
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.ConverterScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.HesabgarTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val calcState by viewModel.calcState.collectAsStateWithLifecycle()

            HesabgarTheme(themeMode = prefs.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HesabgarAppShell(
                        prefs = prefs,
                        selectedTab = selectedTab,
                        onSelectTab = viewModel::selectTab,
                        calcContent = { showMessage, openSettings ->
                            CalculatorScreen(
                                state = calcState,
                                prefs = prefs,
                                onDigitOrDot = viewModel::appendDigitOrDecimal,
                                onOperator = viewModel::appendOperator,
                                onFunction = viewModel::appendFunction,
                                onConstant = viewModel::appendConstant,
                                onSmartParen = viewModel::appendSmartParenthesis,
                                onToggleSign = viewModel::toggleSign,
                                onReciprocal = viewModel::reciprocal,
                                onBackspace = viewModel::backspace,
                                onClear = viewModel::clearAll,
                                onEquals = viewModel::evaluateEquals,
                                onToggleScientific = viewModel::toggleScientific,
                                onToggleInverseTrig = viewModel::toggleInverseTrig,
                                onToggleAngleUnit = viewModel::toggleAngleUnit,
                                onMemoryClear = viewModel::memoryClear,
                                onMemoryAdd = viewModel::memoryAdd,
                                onMemorySubtract = viewModel::memorySubtract,
                                onMemoryRecall = viewModel::memoryRecall,
                                onLoadHistory = viewModel::loadFromHistory,
                                onInsertHistory = viewModel::insertFromHistory,
                                onDeleteHistory = viewModel::deleteHistoryItem,
                                onClearHistory = viewModel::clearHistory,
                                onSaveCurrentResult = viewModel::saveCurrentResultWithName,
                                onSaveHistoryWithName = viewModel::saveHistoryEntryWithName,
                                onInsertSavedResult = viewModel::insertSavedResult,
                                onLoadSavedResult = viewModel::loadFromSavedResult,
                                onRenameSavedResult = viewModel::renameSavedResult,
                                onDeleteSavedResult = viewModel::deleteSavedResult,
                                onOpenSettings = openSettings,
                                onShowMessage = showMessage
                            )
                        },
                        financeContent = { FinanceScreen(prefs = prefs) },
                        converterContent = { ConverterScreen(prefs = prefs) },
                        toolsContent = { ToolsScreen(prefs = prefs) },
                        onThemeModeChange = viewModel::setThemeMode,
                        onPersianDigitsChange = viewModel::setUsePersianDigits,
                        onHapticChange = viewModel::setHapticEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun HesabgarAppShell(
    prefs: UserPreferences,
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    calcContent: @Composable (showMessage: (String) -> Unit, openSettings: () -> Unit) -> Unit,
    financeContent: @Composable () -> Unit,
    converterContent: @Composable () -> Unit,
    toolsContent: @Composable () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onPersianDigitsChange: (Boolean) -> Unit,
    onHapticChange: (Boolean) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (selectedTab != AppTab.CALCULATOR) {
        BackHandler {
            onSelectTab(AppTab.CALCULATOR)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.titleFa,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.titleFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.titleFa
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.titleFa,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Crossfade(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f),
                    label = "tab_crossfade"
                ) { tab ->
                    when (tab) {
                        AppTab.CALCULATOR -> calcContent(showMessage) { showSettingsDialog = true }
                        AppTab.FINANCE -> financeContent()
                        AppTab.CONVERTER -> converterContent()
                        AppTab.TOOLS -> toolsContent()
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "تنظیمات حسابگر فیروزه",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "تم ظاهری",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mode.titleFa, style = MaterialTheme.typography.bodyLarge)
                            RadioButton(
                                selected = prefs.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("نمایش ارقام فارسی (۱۲۳)", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "در صورت خاموش بودن: 123",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.usePersianDigits,
                            onCheckedChange = onPersianDigitsChange,
                            modifier = Modifier.testTag("persian_digits_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("لرزش ملایم کلیدها", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = prefs.hapticEnabled,
                            onCheckedChange = onHapticChange,
                            modifier = Modifier.testTag("haptic_switch")
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSettingsDialog = false },
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Text("تأیید")
                }
            }
        )
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.CALCULATOR -> if (selected) Icons.Filled.Calculate else Icons.Outlined.Calculate
    AppTab.FINANCE -> if (selected) Icons.Filled.AccountBalance else Icons.Outlined.AccountBalance
    AppTab.CONVERTER -> if (selected) Icons.Filled.SwapHoriz else Icons.Outlined.SwapHoriz
    AppTab.TOOLS -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
}
