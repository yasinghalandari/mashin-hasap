package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.HistoryEntry
import com.example.data.SavedResultEntry
import com.example.data.UserPreferences
import com.example.domain.JalaliCalendar
import com.example.domain.PersianFormatter
import com.example.ui.CalculatorUiState
import com.example.ui.components.PersianShamsehBackdrop
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHesabgarColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    prefs: UserPreferences,
    onDigitOrDot: (String) -> Unit,
    onOperator: (String) -> Unit,
    onFunction: (String) -> Unit,
    onConstant: (String) -> Unit,
    onSmartParen: () -> Unit,
    onToggleSign: () -> Unit,
    onReciprocal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onToggleScientific: () -> Unit,
    onToggleInverseTrig: () -> Unit,
    onToggleAngleUnit: () -> Unit,
    onMemoryClear: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySubtract: () -> Unit,
    onMemoryRecall: () -> Unit,
    onLoadHistory: (HistoryEntry) -> Unit,
    onInsertHistory: (HistoryEntry) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSaveCurrentResult: (String) -> Boolean,
    onSaveHistoryWithName: (HistoryEntry, String) -> Boolean,
    onInsertSavedResult: (SavedResultEntry) -> Unit,
    onLoadSavedResult: (SavedResultEntry) -> Unit,
    onRenameSavedResult: (String, String) -> Unit,
    onDeleteSavedResult: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalHesabgarColors.current
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.copied_toast)

    var showHistorySheet by remember { mutableStateOf(false) }
    var sheetInitialTab by remember { mutableIntStateOf(0) } // 0 = history, 1 = saved
    var showSaveCurrentDialog by remember { mutableStateOf(false) }
    var historyItemToSave by remember { mutableStateOf<HistoryEntry?>(null) }
    var savedItemToRename by remember { mutableStateOf<SavedResultEntry?>(null) }

    val hasSavableResult = state.liveResult.isNotEmpty() ||
        (state.justEvaluated && state.expression.isNotEmpty())

    fun performHaptic() {
        if (prefs.hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display & Persian Shamseh Header Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .heightIn(min = 160.dp),
                shape = RoundedCornerShape(26.dp),
                color = extended.displayCardBg,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                tonalElevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    PersianShamsehBackdrop(
                        accentColor = extended.patternTint,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top action strip inside display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = state.isScientificExpanded,
                                    onClick = {
                                        performHaptic()
                                        onToggleScientific()
                                    },
                                    label = {
                                        Text(
                                            text = "مهندسی",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Science,
                                            contentDescription = stringResource(R.string.cd_toggle_scientific),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("toggle_scientific_chip")
                                )

                                Surface(
                                    onClick = {
                                        performHaptic()
                                        onToggleAngleUnit()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("angle_unit_chip")
                                ) {
                                    Text(
                                        text = prefs.angleUnit.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                if (state.memoryValue != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "M",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        performHaptic()
                                        sheetInitialTab = 1
                                        showHistorySheet = true
                                    },
                                    modifier = Modifier.testTag("saved_results_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Bookmarks,
                                        contentDescription = "نتایج ذخیره‌شده",
                                        tint = if (prefs.savedResults.isNotEmpty()) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        performHaptic()
                                        sheetInitialTab = 0
                                        showHistorySheet = true
                                    },
                                    modifier = Modifier.testTag("history_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = stringResource(R.string.cd_history),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        performHaptic()
                                        onOpenSettings()
                                    },
                                    modifier = Modifier.testTag("settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.cd_settings),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Expression & Result Area
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalAlignment = Alignment.End
                        ) {
                            val displayExpr = PersianFormatter.formatExpressionForDisplay(
                                state.expression,
                                prefs.usePersianDigits
                            )
                            val exprScroll = rememberScrollState()

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(exprScroll),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (displayExpr.isEmpty()) {
                                        Text(
                                            text = PersianFormatter.localizeDigits("0", prefs.usePersianDigits),
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                fontSize = 40.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            ),
                                            textAlign = TextAlign.End
                                        )
                                    } else {
                                        Text(
                                            text = colorizeExpression(
                                                expr = displayExpr,
                                                baseColor = MaterialTheme.colorScheme.onSurface,
                                                operatorColor = MaterialTheme.colorScheme.primary,
                                                accentColor = MaterialTheme.colorScheme.secondary
                                            ),
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                fontSize = when {
                                                    displayExpr.length > 18 -> 26.sp
                                                    displayExpr.length > 12 -> 32.sp
                                                    else -> 40.sp
                                                }
                                            ),
                                            maxLines = 1,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.testTag("calculator_expression_text")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (state.errorMessage != null) {
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("calculator_error_text")
                                )
                            } else if (state.liveResult.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            performHaptic()
                                            showSaveCurrentDialog = true
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("save_named_result_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.BookmarkAdd,
                                            contentDescription = "ذخیره نتیجه با نام",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(state.liveResult))
                                            onShowMessage(copiedMessage)
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("copy_live_result_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = stringResource(R.string.cd_copy_result),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = "= ${state.liveResult}",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontSize = 24.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("calculator_live_result")
                                        )
                                    }
                                }
                            } else if (hasSavableResult) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            performHaptic()
                                            showSaveCurrentDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("save_named_result_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.BookmarkAdd,
                                            contentDescription = "ذخیره با نام",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ذخیره با نام",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(displayExpr))
                                            onShowMessage(copiedMessage)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("copy_final_result_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = stringResource(R.string.cd_copy_result),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("کپی", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            if (state.liveWords.isNotEmpty()) {
                                Text(
                                    text = state.liveWords,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                        .testTag("calculator_words_caption")
                                )
                            }
                        }
                    }
                }
            }

            // Quick Reusable Named Results Strip (appears when user has saved named results)
            AnimatedVisibility(visible = prefs.savedResults.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prefs.savedResults, key = { it.id }) { saved ->
                        Surface(
                            onClick = {
                                performHaptic()
                                onInsertSavedResult(saved)
                                onShowMessage("«${saved.name}» در محاسبه درج شد")
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("quick_saved_chip_${saved.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = saved.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = saved.resultText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Scientific & Memory Keypad
            AnimatedVisibility(
                visible = state.isScientificExpanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallSciKey("MC", Modifier.weight(1f)) { performHaptic(); onMemoryClear() }
                        SmallSciKey("MR", Modifier.weight(1f)) { performHaptic(); onMemoryRecall() }
                        SmallSciKey("M+", Modifier.weight(1f)) { performHaptic(); onMemoryAdd() }
                        SmallSciKey("M-", Modifier.weight(1f)) { performHaptic(); onMemorySubtract() }
                        SmallSciKey(
                            text = if (state.isInverseTrig) "2nd●" else "2nd",
                            isHighlighted = state.isInverseTrig,
                            modifier = Modifier.weight(1f)
                        ) { performHaptic(); onToggleInverseTrig() }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallSciKey(if (state.isInverseTrig) "asin" else "sin", Modifier.weight(1f)) {
                            performHaptic()
                            onFunction(if (state.isInverseTrig) "asin(" else "sin(")
                        }
                        SmallSciKey(if (state.isInverseTrig) "acos" else "cos", Modifier.weight(1f)) {
                            performHaptic()
                            onFunction(if (state.isInverseTrig) "acos(" else "cos(")
                        }
                        SmallSciKey(if (state.isInverseTrig) "atan" else "tan", Modifier.weight(1f)) {
                            performHaptic()
                            onFunction(if (state.isInverseTrig) "atan(" else "tan(")
                        }
                        SmallSciKey("ln", Modifier.weight(1f)) { performHaptic(); onFunction("ln(") }
                        SmallSciKey("log", Modifier.weight(1f)) { performHaptic(); onFunction("log(") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallSciKey("√", Modifier.weight(1f)) { performHaptic(); onFunction("sqrt(") }
                        SmallSciKey("xʸ", Modifier.weight(1f)) { performHaptic(); onOperator("^") }
                        SmallSciKey("π", Modifier.weight(1f)) { performHaptic(); onConstant("π") }
                        SmallSciKey("e", Modifier.weight(1f)) { performHaptic(); onConstant("e") }
                        SmallSciKey("n!", Modifier.weight(1f)) { performHaptic(); onConstant("!") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallSciKey("1/x", Modifier.weight(1f)) { performHaptic(); onReciprocal() }
                        SmallSciKey("|x|", Modifier.weight(1f)) { performHaptic(); onFunction("abs(") }
                        SmallSciKey("(", Modifier.weight(1f)) { performHaptic(); onConstant("(") }
                        SmallSciKey(")", Modifier.weight(1f)) { performHaptic(); onConstant(")") }
                        SmallSciKey("±", Modifier.weight(1f)) { performHaptic(); onToggleSign() }
                    }
                }
            }

            // Main Calculator Keypad (LTR numeric layout)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val keyHeight = when {
                        state.isScientificExpanded && prefs.savedResults.isNotEmpty() -> 48.dp
                        state.isScientificExpanded -> 52.dp
                        prefs.savedResults.isNotEmpty() -> 58.dp
                        else -> 64.dp
                    }
                    val d: (String) -> String = { PersianFormatter.localizeDigits(it, prefs.usePersianDigits) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcButton(
                            label = "AC",
                            containerColor = extended.clearKeyBg,
                            contentColor = extended.clearKeyText,
                            height = keyHeight,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("key_ac"),
                            onClick = { performHaptic(); onClear() }
                        )
                        CalcButton(
                            label = "( )",
                            containerColor = extended.functionKeyBg,
                            contentColor = extended.functionKeyText,
                            height = keyHeight,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("key_paren"),
                            onClick = { performHaptic(); onSmartParen() }
                        )
                        CalcButton(
                            label = "%",
                            containerColor = extended.functionKeyBg,
                            contentColor = extended.functionKeyText,
                            height = keyHeight,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("key_percent"),
                            onClick = { performHaptic(); onConstant("%") }
                        )
                        CalcButton(
                            label = "÷",
                            containerColor = extended.operatorKeyBg,
                            contentColor = extended.operatorKeyText,
                            height = keyHeight,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("key_divide"),
                            onClick = { performHaptic(); onOperator("÷") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcButton(d("7"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_7")) {
                            performHaptic(); onDigitOrDot("7")
                        }
                        CalcButton(d("8"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_8")) {
                            performHaptic(); onDigitOrDot("8")
                        }
                        CalcButton(d("9"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_9")) {
                            performHaptic(); onDigitOrDot("9")
                        }
                        CalcButton("×", extended.operatorKeyBg, extended.operatorKeyText, keyHeight, Modifier.weight(1f).testTag("key_multiply")) {
                            performHaptic(); onOperator("×")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcButton(d("4"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_4")) {
                            performHaptic(); onDigitOrDot("4")
                        }
                        CalcButton(d("5"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_5")) {
                            performHaptic(); onDigitOrDot("5")
                        }
                        CalcButton(d("6"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_6")) {
                            performHaptic(); onDigitOrDot("6")
                        }
                        CalcButton("−", extended.operatorKeyBg, extended.operatorKeyText, keyHeight, Modifier.weight(1f).testTag("key_minus")) {
                            performHaptic(); onOperator("-")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcButton(d("1"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_1")) {
                            performHaptic(); onDigitOrDot("1")
                        }
                        CalcButton(d("2"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_2")) {
                            performHaptic(); onDigitOrDot("2")
                        }
                        CalcButton(d("3"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_3")) {
                            performHaptic(); onDigitOrDot("3")
                        }
                        CalcButton("+", extended.operatorKeyBg, extended.operatorKeyText, keyHeight, Modifier.weight(1f).testTag("key_plus")) {
                            performHaptic(); onOperator("+")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcButton(d("0"), extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_0")) {
                            performHaptic(); onDigitOrDot("0")
                        }
                        CalcButton(".", extended.numberKeyBg, extended.numberKeyText, keyHeight, Modifier.weight(1f).testTag("key_dot")) {
                            performHaptic(); onDigitOrDot(".")
                        }
                        Surface(
                            onClick = { performHaptic(); onBackspace() },
                            shape = RoundedCornerShape(18.dp),
                            color = extended.functionKeyBg,
                            modifier = Modifier
                                .weight(1f)
                                .height(keyHeight)
                                .testTag("key_backspace")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = stringResource(R.string.cd_backspace),
                                    tint = extended.functionKeyText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        CalcButton(
                            label = "=",
                            containerColor = extended.equalsKeyBg,
                            contentColor = extended.equalsKeyText,
                            height = keyHeight,
                            fontSize = 28,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("key_equals"),
                            onClick = { performHaptic(); onEquals() }
                        )
                    }
                }
            }
        }
    }

    // History & Named Saved Results ModalBottomSheet
    if (showHistorySheet) {
        var activeSheetTab by remember(sheetInitialTab) { mutableIntStateOf(sheetInitialTab) }

        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeSheetTab == 0,
                        onClick = { activeSheetTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("sheet_tab_history")
                    ) {
                        Text(
                            text = "تاریخچه (${PersianFormatter.localizeDigits(prefs.history.size.toString(), prefs.usePersianDigits)})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    SegmentedButton(
                        selected = activeSheetTab == 1,
                        onClick = { activeSheetTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("sheet_tab_saved")
                    ) {
                        Text(
                            text = "ذخیره‌شده‌ها (${PersianFormatter.localizeDigits(prefs.savedResults.size.toString(), prefs.usePersianDigits)})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (activeSheetTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "محاسبات اخیر",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (prefs.history.isNotEmpty()) {
                            TextButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "پاک کردن همه",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("پاک کردن همه")
                            }
                        }
                    }

                    if (prefs.history.isEmpty()) {
                        EmptyArchiveView(
                            title = "هنوز محاسبه‌ای ثبت نشده است",
                            subtitle = "با زدن دکمه = نتیجه محاسبات شما به‌طور خودکار در تاریخچه ذخیره می‌شود"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(prefs.history, key = { it.id }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onLoadHistory(item)
                                            showHistorySheet = false
                                        }
                                        .testTag("history_item_${item.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { historyItemToSave = item },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .testTag("bookmark_history_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.BookmarkAdd,
                                                    contentDescription = "ذخیره با نام",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    onInsertHistory(item)
                                                    showHistorySheet = false
                                                    onShowMessage("مقدار در محاسبه درج شد")
                                                },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AddCircleOutline,
                                                    contentDescription = "درج در محاسبه",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteHistory(item.id) },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.DeleteOutline,
                                                    contentDescription = "حذف",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = formatHistoryTimestamp(item.timestampMillis, prefs.usePersianDigits),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                Text(
                                                    text = item.expression,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "= ${item.resultText}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "نتایج نام‌گذاری‌شده (برای استفاده مجدد)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (prefs.savedResults.isEmpty()) {
                        EmptyArchiveView(
                            title = "هنوز نتیجه‌ای با نام ذخیره نکرده‌اید",
                            subtitle = "روی آیکون نشانه‌گذاری کنار نتیجه محاسبه بزنید تا با نام دلخواه (مثل حقوق یا اجاره) ذخیره شود"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(prefs.savedResults, key = { it.id }) { saved ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("saved_item_${saved.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Bookmark,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = saved.name,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = { savedItemToRename = saved },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Edit,
                                                        contentDescription = "تغییر نام",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDeleteSavedResult(saved.id) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.DeleteOutline,
                                                        contentDescription = "حذف",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                if (saved.expression.isNotEmpty()) {
                                                    Text(
                                                        text = saved.expression,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = saved.resultText,
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    onInsertSavedResult(saved)
                                                    showHistorySheet = false
                                                    onShowMessage("«${saved.name}» در عبارت فعلی درج شد")
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("insert_saved_${saved.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AddCircleOutline,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("درج در عبارت")
                                            }

                                            TextButton(
                                                onClick = {
                                                    onLoadSavedResult(saved)
                                                    showHistorySheet = false
                                                    onShowMessage("«${saved.name}» بارگذاری شد")
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("load_saved_${saved.id}")
                                            ) {
                                                Text("شروع با این عدد")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Save current calculator result with a custom name
    if (showSaveCurrentDialog) {
        SaveNameDialog(
            title = "ذخیره نتیجه با نام دلخواه",
            initialName = "",
            previewValue = state.liveResult.ifEmpty {
                PersianFormatter.formatExpressionForDisplay(state.expression, prefs.usePersianDigits)
            },
            onDismiss = { showSaveCurrentDialog = false },
            onConfirm = { name ->
                if (onSaveCurrentResult(name)) {
                    showSaveCurrentDialog = false
                    onShowMessage("نتیجه با نام «$name» ذخیره شد")
                }
            }
        )
    }

    // Dialog: Save a history item with a custom name
    historyItemToSave?.let { entry ->
        SaveNameDialog(
            title = "ذخیره از تاریخچه با نام",
            initialName = "",
            previewValue = entry.resultText,
            onDismiss = { historyItemToSave = null },
            onConfirm = { name ->
                if (onSaveHistoryWithName(entry, name)) {
                    historyItemToSave = null
                    onShowMessage("با نام «$name» ذخیره شد")
                }
            }
        )
    }

    // Dialog: Rename an existing saved result
    savedItemToRename?.let { saved ->
        SaveNameDialog(
            title = "تغییر نام نتیجه",
            initialName = saved.name,
            previewValue = saved.resultText,
            onDismiss = { savedItemToRename = null },
            onConfirm = { newName ->
                onRenameSavedResult(saved.id, newName)
                savedItemToRename = null
                onShowMessage("نام به «$newName» تغییر یافت")
            }
        )
    }
}

@Composable
private fun EmptyArchiveView(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Calculate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SaveNameDialog(
    title: String,
    initialName: String,
    previewValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    val suggestions = listOf("حقوق", "اجاره", "قسط", "فاکتور", "قیمت طلا", "بودجه")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مقدار:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = previewValue,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("نام دلخواه (مثلاً اجاره خانه)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_name_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        FilterChip(
                            selected = nameInput == suggestion,
                            onClick = { nameInput = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameInput) },
                enabled = nameInput.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_save_name_button")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

private fun formatHistoryTimestamp(millis: Long, usePersianDigits: Boolean): String {
    if (millis <= 0L) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val jDate = JalaliCalendar.gregorianToJalali(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    val hour = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    val monthName = JalaliCalendar.jalaliMonths.getOrElse(jDate.month - 1) { "" }
    val raw = "${jDate.day} $monthName • $hour:$min"
    return PersianFormatter.localizeDigits(raw, usePersianDigits)
}

@Composable
private fun CalcButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    fontSize: Int = 22,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "key_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = modifier
            .height(height)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SmallSciKey(
    text: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val extended = LocalHesabgarColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else extended.functionKeyBg,
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp
                ),
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else extended.functionKeyText,
                maxLines = 1
            )
        }
    }
}

private fun colorizeExpression(
    expr: String,
    baseColor: Color,
    operatorColor: Color,
    accentColor: Color
): AnnotatedString = buildAnnotatedString {
    for (ch in expr) {
        when (ch) {
            '+', '-', '−', '×', '÷', '^', '%' -> {
                withStyle(SpanStyle(color = operatorColor, fontWeight = FontWeight.Bold)) {
                    append(ch)
                }
            }
            '(', ')', 'π' -> {
                withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold)) {
                    append(ch)
                }
            }
            else -> {
                withStyle(SpanStyle(color = baseColor)) {
                    append(ch)
                }
            }
        }
    }
}
