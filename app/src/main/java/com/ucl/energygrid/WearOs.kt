package com.ucl.energygrid

import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment

import com.ucl.energygrid.ui.screen.Mine

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface


@Composable
fun SiteInformationPanelWear(mine: Mine, userId: Int, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = mine.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C0471)
                )
                // 其他內容
            }
            // 其他 item
        }
    }
}
