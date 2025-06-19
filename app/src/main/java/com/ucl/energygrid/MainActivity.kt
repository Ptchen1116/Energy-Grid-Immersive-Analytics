package com.ucl.energygrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ucl.energygrid.ui.screen.MainScreen

import android.widget.ImageView
import android.view.ViewGroup
import android.content.res.Resources
import android.widget.FrameLayout
import com.ucl.energygrid.ui.component.PinType
import com.ucl.energygrid.ui.component.addPin
import android.view.ViewGroup.MarginLayoutParams

/* class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
} */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pin)

        val rootLayout = findViewById<FrameLayout>(R.id.pinLayout)

        addPin(this, rootLayout, 50, 100, 48, 54, PinType.MINE)
        addPin(this, rootLayout, 150, 100, 48, 54, PinType.SOLAR)
        addPin(this, rootLayout, 250, 100, 48, 54, PinType.WIND)
        addPin(this, rootLayout, 350, 100, 48, 54, PinType.HYDROELECTRIC)
    }
}



