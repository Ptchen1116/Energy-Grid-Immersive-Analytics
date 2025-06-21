package com.ucl.energygrid.ui.component

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.ucl.energygrid.R

enum class PinType {
    MINE,
    SOLAR,
    WIND,
    HYDROELECTRIC,
    FLOODING_RISK
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
        PinType.FLOODING_RISK -> R.drawable.flooding_risk
    }

    val pinColor = when (type) {
        PinType.MINE -> "#F44336".toColorInt()
        PinType.SOLAR -> "#FFA000".toColorInt()
        PinType.WIND -> "#4CAF50".toColorInt()
        PinType.HYDROELECTRIC -> "#2196F3".toColorInt()
        PinType.FLOODING_RISK -> "#F44336".toColorInt()
    }

    val pinSizePx = widthDp.dpToPx(context)

    val pinBitmap = Bitmap.createBitmap(pinSizePx, pinSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(pinBitmap)
    val paint = Paint().apply {
        color = pinColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.dpToPx(context).toFloat()
        isAntiAlias = true
    }

    val radius = pinSizePx / 2f
    canvas.drawCircle(radius, radius, radius - 2.dpToPx(context), paint)
    canvas.drawCircle(radius, radius, radius - 2.dpToPx(context), strokePaint)

    val pinContainer = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            pinSizePx, pinSizePx
        ).apply {
            setMargins(xDp.dpToPx(context), yDp.dpToPx(context), 0, 0)
        }
    }

    val pinImage = ImageView(context).apply {
        setImageBitmap(pinBitmap)
        contentDescription = "Location pin"
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    val iconOverlay = ImageView(context).apply {
        setImageResource(iconResId)
        setColorFilter(Color.WHITE)
        contentDescription = "${type.name.lowercase()} icon"
        layoutParams = FrameLayout.LayoutParams(
            18.dpToPx(context), 18.dpToPx(context)
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
    }

    pinContainer.addView(pinImage)
    pinContainer.addView(iconOverlay)
    parent.addView(pinContainer)
}

fun createPinBitmap(context: Context, type: PinType): Bitmap {
    val frameLayout = FrameLayout(context)
    addPin(
        context = context,
        parent = frameLayout,
        xDp = 0,
        yDp = 0,
        widthDp = 36, // ⬅️ smaller pin
        heightDp = 36,
        type = type
    )

    // Measure and layout the view
    val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    frameLayout.measure(spec, spec)
    frameLayout.layout(0, 0, frameLayout.measuredWidth, frameLayout.measuredHeight)

    // Create Bitmap and draw view onto it
    val bitmap = Bitmap.createBitmap(
        frameLayout.measuredWidth,
        frameLayout.measuredHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    frameLayout.draw(canvas)

    return bitmap
}