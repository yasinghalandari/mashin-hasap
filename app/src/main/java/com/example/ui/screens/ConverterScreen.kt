package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UserPreferences
import com.example.domain.PersianFormatter
import com.example.domain.UnitCategory
import com.example.domain.UnitConverterEngine

@Composable
fun ConverterScreen(
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf(UnitCategory.LENGTH) }
    val units = remember(selectedCategory) { UnitConverterEngine.unitsFor(selectedCategory) }
    var fromIndex by rememberSaveable(selectedCategory) { mutableIntStateOf(0) }
    var toIndex by rememberSaveable(selectedCategory) { mutableIntStateOf(if (units.size > 1) 1 else 0) }
    var inputText by rememberSaveable { mutableStateOf("1") }

    val fromUnit = units.getOrElse(fromIndex) { units.first() }
    val toUnit = units.getOrElse(toIndex) { units.first() }
    val inputValue = PersianFormatter.parseDoubleOrNull(inputText) ?: 0.0
    val converted = remember(inputValue, selectedCategory, fromUnit, toUnit) {
        UnitConverterEngine.convert(inputValue, selectedCategory, fromUnit, toUnit)
    }

    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "تبدیل واحد جامع",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "از متر و کیلوگرم تا مثقال، سوت و هکتار",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Category chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnitCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            fromIndex = 0
                            toIndex = 1
                        },
                        label = { Text(cat.titleFa) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("unit_category_${cat.name.lowercase()}")
                    )
                }
            }

            // Source & Target unit card with Swap button
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // From Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "واحد مبدأ",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box {
                                    FilterChip(
                                        selected = true,
                                        onClick = { showFromMenu = true },
                                        label = { Text("${fromUnit.nameFa} (${fromUnit.symbol})") },
                                        modifier = Modifier.testTag("from_unit_selector")
                                    )
                                    DropdownMenu(
                                        expanded = showFromMenu,
                                        onDismissRequest = { showFromMenu = false }
                                    ) {
                                        units.forEachIndexed { idx, unit ->
                                            DropdownMenuItem(
                                                text = { Text("${unit.nameFa} (${unit.symbol})") },
                                                onClick = {
                                                    fromIndex = idx
                                                    showFromMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            NumericInputField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = "مقدار ورودی",
                                testTag = "converter_input"
                            )
                        }
                    }

                    // To Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "واحد مقصد",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box {
                                    FilterChip(
                                        selected = true,
                                        onClick = { showToMenu = true },
                                        label = { Text("${toUnit.nameFa} (${toUnit.symbol})") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.testTag("to_unit_selector")
                                    )
                                    DropdownMenu(
                                        expanded = showToMenu,
                                        onDismissRequest = { showToMenu = false }
                                    ) {
                                        units.forEachIndexed { idx, unit ->
                                            DropdownMenuItem(
                                                text = { Text("${unit.nameFa} (${unit.symbol})") },
                                                onClick = {
                                                    toIndex = idx
                                                    showToMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "${PersianFormatter.formatNumber(converted, prefs.usePersianDigits)} ${toUnit.symbol}",
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("converter_result_text")
                            )
                        }
                    }
                }

                // Floating Swap Button
                Surface(
                    onClick = {
                        val prev = fromIndex
                        fromIndex = toIndex
                        toIndex = prev
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .testTag("swap_units_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = stringResource(R.string.cd_swap_units),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }

            // All units in this category at a glance
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "معادل در همه واحدهای ${selectedCategory.titleFa}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    units.forEach { target ->
                        val valInUnit = UnitConverterEngine.convert(inputValue, selectedCategory, fromUnit, target)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${target.nameFa} (${target.symbol})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = PersianFormatter.formatNumber(valInUnit, prefs.usePersianDigits, maxFractionDigits = 6),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (target.id == toUnit.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
