package com.example.myapplication.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.component.TabSelectionIndicator


@Composable
fun TimeSimulationPanel(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var energyDemandHeatmap by remember { mutableStateOf(false) }
    var startYear by remember { mutableStateOf("2025") }
    var endYear by remember { mutableStateOf("2035") }
    val selectedColor = Color(0xFF03045E)
    val unselectedColor = Color(0xFF8E8E93)

    val years = (2020..2040).map { it.toString() }

    val startYearInt = startYear.toIntOrNull() ?: 2025
    val endYearInt = endYear.toIntOrNull() ?: 2035

    val yearRange = if (startYearInt <= endYearInt) (startYearInt..endYearInt).toList()
    else (endYearInt..startYearInt).toList()

    var sliderIndex by remember { mutableFloatStateOf(0f) }

    sliderIndex = sliderIndex.coerceIn(0f, (yearRange.size - 1).toFloat())

    val selectedYear = yearRange[sliderIndex.toInt()]

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    TabSelectionIndicator(
                        color = selectedColor,
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Data Overlay",
                            color = if (selectedTab == 0) selectedColor else unselectedColor
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Parameters",
                            color = if (selectedTab == 1) selectedColor else unselectedColor
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = energyDemandHeatmap,
                            onCheckedChange = { energyDemandHeatmap = it }
                        )
                        Text("Energy Demand Heat map")
                    }
                }

                1 -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Simulation Year: $selectedYear")

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuBox(
                            selectedOption = startYear,
                            options = years,
                            onOptionSelected = { startYear = it }
                        )

                        Slider(
                            value = sliderIndex,
                            onValueChange = { sliderIndex = it },
                            valueRange = 0f..(yearRange.lastIndex).toFloat(),
                            steps = yearRange.size - 2,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )

                        DropdownMenuBox(
                            selectedOption = endYear,
                            options = years,
                            onOptionSelected = { endYear = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuBox(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedOption)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}