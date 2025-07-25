package com.ucl.energygrid.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ucl.energygrid.R
import com.ucl.energygrid.data.model.BottomSheetContent
import com.ucl.energygrid.ui.component.BottomNavItem


@Composable
fun BottomNavigationBar(
    selectedItem: BottomSheetContent,
    modifier: Modifier = Modifier,
    onMapControlClick: () -> Unit,
    onSiteInfoClick: () -> Unit,
    onSimulationClick: () -> Unit,
) {
    Surface(
        color = Color(0xFFAAE5F2),
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 15.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                iconRes = R.drawable.layers_btn,
                label = "Map Control",
                isSelected = selectedItem == BottomSheetContent.MapControl,
                onClick = onMapControlClick
            )
            BottomNavItem(
                iconRes = R.drawable.description_btn,
                label = "Site Information",
                isSelected = selectedItem == BottomSheetContent.SiteInfo,
                onClick = onSiteInfoClick
            )
            BottomNavItem(
                iconRes = R.drawable.query_stats_btn,
                label = "Time Simulation",
                isSelected = selectedItem == BottomSheetContent.TimeSimulation,
                onClick = onSimulationClick
            )
        }
    }
}
