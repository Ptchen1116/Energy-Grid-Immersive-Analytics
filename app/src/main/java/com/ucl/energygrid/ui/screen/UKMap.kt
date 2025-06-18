package com.ucl.energygrid.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ucl.energygrid.R

@Composable
fun UKMap(
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.uk_map),
            contentDescription = "UK Map",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )

        FloatingActionButton(
            onClick = { /* Optional 功能 */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 128.dp),
            containerColor = Color.White,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.map_btn),
                contentDescription = "Map Button",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}