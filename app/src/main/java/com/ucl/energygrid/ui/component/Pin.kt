package com.ucl.energygrid.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.ucl.energygrid.R
import com.ucl.energygrid.data.model.PinType
import com.ucl.energygrid.data.model.Trend


fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

fun createPinBitmap(
    context: Context,
    type: PinType,
    trend: Trend? = null
): Bitmap {
    val frameLayout = FrameLayout(context)

    val pinColor = when(trend) {
        Trend.INCREASING -> Color.parseColor("#F44336")
        Trend.DECREASING -> Color.parseColor("#4CAF50")
        Trend.STABLE -> Color.parseColor("#FFEB3B")
        null -> when(type) {
            PinType.USER_PIN -> Color.parseColor("#F44336")
            PinType.CLOSED_MINE -> Color.parseColor("#F44336")
            PinType.CLOSING_MINE -> Color.parseColor("#F44336")
            PinType.SOLAR -> Color.parseColor("#FFA000")
            PinType.WIND -> Color.parseColor("#4CAF50")
            PinType.HYDROELECTRIC -> Color.parseColor("#2196F3")
            PinType.FLOODING_RISK -> Color.parseColor("#F44336")
        }
    }
    Log.d("createPinBitmap", "pinColor: ${String.format("#%06X", 0xFFFFFF and pinColor)} for trend=$trend")


    addPinWithColor(
        context = context,
        parent = frameLayout,
        xDp = 0,
        yDp = 0,
        widthDp = 36,
        heightDp = 36,
        type = type,
        pinColor = pinColor
    )

    val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    frameLayout.measure(spec, spec)
    frameLayout.layout(0, 0, frameLayout.measuredWidth, frameLayout.measuredHeight)

    val bitmap = Bitmap.createBitmap(
        frameLayout.measuredWidth,
        frameLayout.measuredHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    frameLayout.draw(canvas)

    return bitmap
}

fun addPinWithColor(
    context: Context,
    parent: ViewGroup,
    xDp: Int,
    yDp: Int,
    widthDp: Int,
    heightDp: Int,
    type: PinType,
    pinColor: Int
) {
    val iconResId = when (type) {
        PinType.USER_PIN -> R.drawable.pin_my_pin
        PinType.CLOSED_MINE -> R.drawable.pin_closed_mine
        PinType.CLOSING_MINE -> R.drawable.pin_closing_mine
        PinType.SOLAR -> R.drawable.solar_site
        PinType.WIND -> R.drawable.wind_site
        PinType.HYDROELECTRIC -> R.drawable.hydroelectric_site
        PinType.FLOODING_RISK -> R.drawable.flooding_risk
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