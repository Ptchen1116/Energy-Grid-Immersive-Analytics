package com.example.myapplication.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun TabSelectionIndicator(color: Color, modifier: Modifier = Modifier) {
    val borderHeight = with(LocalDensity.current) { 2.dp.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        inset(
            left = 0f,
            top = size.height - borderHeight,
            right = 0f,
            bottom = 0f
        ) {
            drawRect(
                color = color,
                size = Size(size.width, borderHeight)
            )
        }
    }
}