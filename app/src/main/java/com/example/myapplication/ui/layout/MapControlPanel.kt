package com.example.myapplication.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun MapControlPanel(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var closedMine by remember { mutableStateOf(false) }
    var closingMine by remember { mutableStateOf(false) }
    var floodingRisk by remember { mutableStateOf(false) }
    var solar by remember { mutableStateOf(false) }
    var wind by remember { mutableStateOf(false) }
    var hydroelectric by remember { mutableStateOf(false) }

    val selectedColor = Color(0xFF03045E)
    val unselectedColor = Color(0xFF8E8E93)
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = Color.White,
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
                            "Mine Location",
                            color = if (selectedTab == 0) selectedColor else unselectedColor
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Geological Risk",
                            color = if (selectedTab == 1) selectedColor else unselectedColor
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Renewable Sites",
                            color = if (selectedTab == 2) selectedColor else unselectedColor
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = closedMine, onCheckedChange = { closedMine = it })
                        Text("Closed Mine")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = closingMine, onCheckedChange = { closingMine = it })
                        Text("Closing Mine")
                    }
                }
                1 -> Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = floodingRisk, onCheckedChange = { floodingRisk = it })
                        Text("Flooding Risk")
                    }
                }
                2 -> Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = solar, onCheckedChange = { solar = it })
                        Text("Solar")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = wind, onCheckedChange = { wind = it })
                        Text("Wind")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hydroelectric, onCheckedChange = { hydroelectric = it })
                        Text("Hydroelectric")
                    }
                }
            }
        }
    }
}



