package com.ucl.energygrid.ui.component

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.ucl.energygrid.R

enum class PinType {
    MINE,
    SOLAR,
    WIND,
    HYDROELECTRIC
}

fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

fun addPin(
    context: Context,
    parent: ViewGroup,
    xDp: Int,
    yDp: Int,
    widthDp: Int,
    heightDp: Int,
    type: PinType
) {
    val iconResId = when (type) {
        PinType.MINE -> R.drawable.mine_location
        PinType.SOLAR -> R.drawable.solar_site
        PinType.WIND -> R.drawable.wind_site
        PinType.HYDROELECTRIC -> R.drawable.hydroelectric_site
    }

    val pinContainer = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            widthDp.dpToPx(context),
            heightDp.dpToPx(context)
        ).apply {
            setMargins(xDp.dpToPx(context), yDp.dpToPx(context), 0, 0)
        }
    }

    val pinColor = when (type) {
        PinType.MINE -> "#F44336".toColorInt()
        PinType.SOLAR -> "#FFEB3B".toColorInt()
        PinType.WIND -> "#4CAF50".toColorInt()
        PinType.HYDROELECTRIC -> "#2196F3".toColorInt()
    }

    val pinImage = ImageView(context).apply {
        setImageResource(R.drawable.pin_bg)
        setColorFilter(pinColor)
        contentDescription = "Location pin"
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    val iconOverlay = ImageView(context).apply {
        setImageResource(iconResId)
        setColorFilter(android.graphics.Color.WHITE)
        contentDescription = "${type.name.lowercase()} icon"
        layoutParams = FrameLayout.LayoutParams(
            30.dpToPx(context), 30.dpToPx(context)
        ).apply {
            topMargin = 9.dpToPx(context)
            leftMargin = 9.dpToPx(context)
        }
    }

    pinContainer.addView(pinImage)
    pinContainer.addView(iconOverlay)
    parent.addView(pinContainer)
}