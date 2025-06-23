package com.ucl.energygrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ucl.energygrid.ui.screen.MainScreen
import com.ucl.energygrid.data.fetchFloodPolygons
import com.ucl.energygrid.data.loadPolygonPoints
import com.ucl.energygrid.data.loadCenterPoint
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import android.util.Log
import kotlinx.coroutines.launch
import com.ucl.energygrid.data.readAndExtractSitesByType
import com.ucl.energygrid.data.GeoJsonLoader
import com.ucl.energygrid.data.loadEnergyConsumption
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Context
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppEnvironment.isDebug = true

        setContent {
            MainScreen()
        }
    }
}



