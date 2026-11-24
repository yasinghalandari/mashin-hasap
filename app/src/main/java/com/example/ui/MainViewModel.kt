package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryEntry
import com.example.data.PreferencesRepository
import com.example.data.SavedResultEntry
import com.example.data.UserPreferences
import com.example.domain.AngleUnit
import com.example.domain.EvalResult
import com.example.domain.MathEvaluator
import com.example.domain.PersianFormatter
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

enum class AppTab(val titleFa: String) {
    CALCULATOR("حسابگر"),
    FINANCE("مالی و طلا"),
    CONVERTER("تبدیل واحد"),
    TOOLS("تاریخ و سلامت")
}

data class CalculatorUiState(
    val expression: String = "",
    val liveResult: String = "",
    val liveWords: String = "",
    val errorMessage: String? = null,
    val isScientificExpanded: Boolean = false,
    val isInverseTrig: Boolean = false,
    val justEvaluated: Boolean = false,
    val memoryValue: Double? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PreferencesRepository(application.applicationContext)

    val preferences: StateFlow<UserPreferences> = repository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences()
    )

    private val _selectedTab = MutableStateFlow(AppTab.CALCULATOR)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _calcState = MutableStateFlow(CalculatorUiState())
    val calcState: StateFlow<CalculatorUiState> = _calcState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.collect { prefs ->
                recomputeLive(_calcState.value.expression, prefs.angleUnit, prefs.usePersianDigits)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun toggleScientific() {
        _calcState.update { it.copy(isScientificExpanded = !it.isScientificExpanded) }
    }

    fun toggleInverseTrig() {
        _calcState.update { it.copy(isInverseTrig = !it.isInverseTrig) }
    }

    fun appendDigitOrDecimal(token: String) {
        _calcState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            if (token == ".") {
                val lastSegment = base.takeLastWhile { it.isDigit() || it == '.' }
                if (lastSegment.contains('.')) return@update state
                val prefix = if (lastSegment.isEmpty()) "0." else "."
                val next = base + prefix
                return@update computeUpdatedState(state.copy(justEvaluated = false), next)
            }
            val next = base + token
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun appendOperator(op: String) {
        _calcState.update { state ->
            val current = state.expression
            if (current.isEmpty()) {
                return@update if (op == "-") {
                    computeUpdatedState(state.copy(justEvaluated = false), "-")
                } else {
                    state
                }
            }
            val operators = setOf('+', '-', '×', '÷', '^', '*', '/')
            val lastChar = current.last()
            val next = if (lastChar in operators) {
                current.dropLast(1) + op
            } else if (lastChar == '(' && op != "-") {
                current
            } else {
                current + op
            }
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun appendFunction(funcPrefix: String) {
        _calcState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            val next = base + funcPrefix
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun appendConstant(symbol: String) {
        _calcState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            val next = base + symbol
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun appendSmartParenthesis() {
        _calcState.update { state ->
            val expr = if (state.justEvaluated) "" else state.expression
            val openCount = expr.count { it == '(' }
            val closeCount = expr.count { it == ')' }
            val last = expr.lastOrNull()
            val canClose = openCount > closeCount &&
                last != null &&
                (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!')
            val next = expr + if (canClose) ")" else "("
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun toggleSign() {
        _calcState.update { state ->
            val expr = state.expression
            if (expr.isEmpty()) {
                return@update computeUpdatedState(state, "-")
            }
            val next = if (expr.startsWith("(-") && expr.endsWith(")")) {
                expr.substring(2, expr.length - 1)
            } else if (expr.startsWith("-") && expr.drop(1).all { it.isDigit() || it == '.' }) {
                expr.drop(1)
            } else if (expr.all { it.isDigit() || it == '.' }) {
                "-$expr"
            } else {
                "(-$expr)"
            }
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun reciprocal() {
        _calcState.update { state ->
            val expr = state.expression
            if (expr.isEmpty()) return@update state
            val next = "1÷($expr)"
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun backspace() {
        _calcState.update { state ->
            if (state.expression.isEmpty()) return@update state
            val funcs = listOf("asin(", "acos(", "atan(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln(", "abs(")
            val matchedFunc = funcs.firstOrNull { state.expression.endsWith(it) }
            val next = if (matchedFunc != null) {
                state.expression.dropLast(matchedFunc.length)
            } else {
                state.expression.dropLast(1)
            }
            computeUpdatedState(state.copy(justEvaluated = false), next)
        }
    }

    fun clearAll() {
        _calcState.value = CalculatorUiState(
            isScientificExpanded = _calcState.value.isScientificExpanded,
            isInverseTrig = _calcState.value.isInverseTrig,
            memoryValue = _calcState.value.memoryValue
        )
    }

    fun evaluateEquals() {
        val currentExpr = _calcState.value.expression
        if (currentExpr.isBlank()) return
        val prefs = preferences.value
        when (val res = MathEvaluator.evaluate(currentExpr, prefs.angleUnit)) {
            is EvalResult.Success -> {
                val formattedPlain = PersianFormatter.formatNumber(
                    value = res.value,
                    usePersianDigits = false,
                    grouping = false
                )
                val formattedDisplay = PersianFormatter.formatNumber(
                    value = res.value,
                    usePersianDigits = prefs.usePersianDigits,
                    grouping = true
                )
                val displayExpr = PersianFormatter.formatExpressionForDisplay(
                    currentExpr,
                    prefs.usePersianDigits
                )
                viewModelScope.launch {
                    repository.addHistoryEntry(
                        HistoryEntry(
                            id = UUID.randomUUID().toString(),
                            expression = displayExpr,
                            resultText = formattedDisplay,
                            numericValue = res.value,
                            timestampMillis = System.currentTimeMillis()
                        )
                    )
                }
                _calcState.update {
                    it.copy(
                        expression = formattedPlain,
                        liveResult = "",
                        liveWords = buildWordsCaption(res.value),
                        errorMessage = null,
                        justEvaluated = true
                    )
                }
            }
            is EvalResult.Error -> {
                _calcState.update { it.copy(errorMessage = res.message) }
            }
            EvalResult.Empty -> Unit
        }
    }

    fun memoryClear() {
        _calcState.update { it.copy(memoryValue = null) }
    }

    fun memoryAdd() {
        val value = currentEvaluatedValue() ?: return
        _calcState.update { it.copy(memoryValue = (it.memoryValue ?: 0.0) + value) }
    }

    fun memorySubtract() {
        val value = currentEvaluatedValue() ?: return
        _calcState.update { it.copy(memoryValue = (it.memoryValue ?: 0.0) - value) }
    }

    fun memoryRecall() {
        val mem = _calcState.value.memoryValue ?: return
        insertNumericValue(mem)
    }

    fun insertNumericValue(value: Double) {
        val plain = PersianFormatter.formatNumber(value, usePersianDigits = false, grouping = false)
        val wrapped = if (value < 0) "($plain)" else plain
        _calcState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            computeUpdatedState(state.copy(justEvaluated = false), base + wrapped)
        }
    }

    fun loadFromHistory(entry: HistoryEntry) {
        val plain = PersianFormatter.formatNumber(entry.numericValue, usePersianDigits = false, grouping = false)
        _calcState.update { state ->
            computeUpdatedState(state.copy(justEvaluated = false), plain)
        }
    }

    fun insertFromHistory(entry: HistoryEntry) {
        insertNumericValue(entry.numericValue)
    }

    fun loadFromSavedResult(entry: SavedResultEntry) {
        val plain = PersianFormatter.formatNumber(entry.numericValue, usePersianDigits = false, grouping = false)
        _calcState.update { state ->
            computeUpdatedState(state.copy(justEvaluated = false), plain)
        }
    }

    fun insertSavedResult(entry: SavedResultEntry) {
        insertNumericValue(entry.numericValue)
    }

    fun saveCurrentResultWithName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val currentExpr = _calcState.value.expression
        val evaluated = currentEvaluatedValue() ?: return false
        val prefs = preferences.value
        val formattedDisplay = PersianFormatter.formatNumber(
            value = evaluated,
            usePersianDigits = prefs.usePersianDigits,
            grouping = true
        )
        val displayExpr = PersianFormatter.formatExpressionForDisplay(currentExpr, prefs.usePersianDigits)
        viewModelScope.launch {
            repository.addSavedResult(
                SavedResultEntry(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    expression = displayExpr,
                    resultText = formattedDisplay,
                    numericValue = evaluated,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
        return true
    }

    fun saveHistoryEntryWithName(entry: HistoryEntry, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        viewModelScope.launch {
            repository.addSavedResult(
                SavedResultEntry(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    expression = entry.expression,
                    resultText = entry.resultText,
                    numericValue = entry.numericValue,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
        return true
    }

    fun renameSavedResult(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renameSavedResult(id, trimmed) }
    }

    fun deleteSavedResult(id: String) {
        viewModelScope.launch { repository.deleteSavedResult(id) }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch { repository.deleteHistoryEntry(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setUsePersianDigits(enabled: Boolean) {
        viewModelScope.launch { repository.setUsePersianDigits(enabled) }
    }

    fun toggleAngleUnit() {
        val next = if (preferences.value.angleUnit == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG
        viewModelScope.launch { repository.setAngleUnit(next) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticEnabled(enabled) }
    }

    fun canSaveCurrentResult(): Boolean = currentEvaluatedValue() != null

    private fun currentEvaluatedValue(): Double? {
        val expr = _calcState.value.expression
        if (expr.isBlank()) return null
        return when (val res = MathEvaluator.evaluate(expr, preferences.value.angleUnit)) {
            is EvalResult.Success -> res.value
            else -> null
        }
    }

    private fun recomputeLive(expr: String, angleUnit: AngleUnit, usePersianDigits: Boolean) {
        _calcState.update { computeUpdatedState(it, expr, angleUnit, usePersianDigits) }
    }

    private fun computeUpdatedState(
        state: CalculatorUiState,
        newExpression: String,
        angleUnit: AngleUnit = preferences.value.angleUnit,
        usePersianDigits: Boolean = preferences.value.usePersianDigits
    ): CalculatorUiState {
        if (newExpression.isBlank()) {
            return state.copy(
                expression = "",
                liveResult = "",
                liveWords = "",
                errorMessage = null
            )
        }
        return when (val res = MathEvaluator.evaluate(newExpression, angleUnit)) {
            is EvalResult.Success -> {
                val formatted = PersianFormatter.formatNumber(
                    value = res.value,
                    usePersianDigits = usePersianDigits,
                    grouping = true
                )
                state.copy(
                    expression = newExpression,
                    liveResult = if (state.justEvaluated) "" else formatted,
                    liveWords = buildWordsCaption(res.value),
                    errorMessage = null
                )
            }
            is EvalResult.Error -> {
                state.copy(
                    expression = newExpression,
                    liveResult = "",
                    liveWords = "",
                    errorMessage = null
                )
            }
            EvalResult.Empty -> state.copy(
                expression = newExpression,
                liveResult = "",
                liveWords = "",
                errorMessage = null
            )
        }
    }

    private fun buildWordsCaption(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        val absVal = abs(value)
        if (absVal < 1.0 || absVal > 999_999_999_999_999.0) return ""
        val whole = value.roundToLong()
        return if (abs(value - whole.toDouble()) < 1e-6) {
            PersianFormatter.numberToPersianWords(whole)
        } else {
            ""
        }
    }
}
