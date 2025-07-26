package com.ucl.energygrid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material.Text
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.Trend
import com.ucl.energygrid.data.repository.WebRtcRepository
import com.ucl.energygrid.data.repository.getAllSiteLabelsReferencesAndNames
import com.ucl.energygrid.data.repository.getInfoByReference
import com.ucl.energygrid.ui.component.TypeTag
import com.ucl.energygrid.ui.layout.siteInformationPanel.EnergyLineChartMP
import com.ucl.energygrid.ui.layout.siteInformationPanel.FloodHistoryChartMP
import com.ucl.energygrid.ui.layout.siteInformationPanel.FloodRiskTag
import com.ucl.energygrid.ui.layout.siteInformationPanel.SectionHeader
import com.ucl.energygrid.ui.layout.siteInformationPanel.TrendTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

class WearMainActivity : ComponentActivity() {
    private val _spokenCommand = MutableStateFlow("No selection")
    val spokenCommand: StateFlow<String> get() = _spokenCommand

    private lateinit var localRenderer: SurfaceViewRenderer
    private lateinit var remoteRenderer: SurfaceViewRenderer
    private lateinit var eglBase: EglBase

    private val asrReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.realwear.wearhf.intent.action.SPEECH_EVENT") {
                val command = intent.getStringExtra("com.realwear.wearhf.intent.extra.COMMAND") ?: return
                _spokenCommand.value = command
            }
        }
    }

    private fun sendCommands(commands: List<String>) {
        val intent = Intent("com.realwear.wearhf.intent.action.OVERRIDE_COMMANDS").apply {
            putExtra("com.realwear.wearhf.intent.extra.SOURCE_PACKAGE", packageName)
            putStringArrayListExtra("com.realwear.wearhf.intent.extra.COMMANDS", ArrayList(commands))
        }
        sendBroadcast(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        eglBase = EglBase.create()
        localRenderer = SurfaceViewRenderer(this)
        remoteRenderer = SurfaceViewRenderer(this)
        localRenderer.setMirror(true)
        remoteRenderer.setMirror(true)

        val viewModel: CallingViewModel by viewModels {
            val repo = WebRtcRepository(applicationContext)
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CallingViewModel(repo) as T
                }
            }
        }

        viewModel.init(localRenderer, remoteRenderer, isCaller = false)

        setContent {
            val command by spokenCommand.collectAsState()
            val context = LocalContext.current
            val sites = remember { getAllSiteLabelsReferencesAndNames(context) }

            var currentStage by remember { mutableStateOf("selectSite") }
            var selectedMineReference by remember { mutableStateOf<String?>(null) }
            var selectedMineInfo by remember { mutableStateOf<Mine?>(null) }

            LaunchedEffect(command) {
                Log.i("WearMainCommand", "Received command: '$command'")
                if (currentStage == "selectSite") {
                    val selectedSite = sites.firstOrNull {
                        it.first.equals(command, ignoreCase = true) ||
                                command in listOf("one", "1", "two", "2", "three", "3") &&
                                it.first.endsWith(command.takeLast(1))
                    }
                    if (selectedSite != null) {
                        selectedMineReference = selectedSite.second
                        selectedMineInfo = getInfoByReference(context, selectedSite.second)
                        currentStage = "menu"

                        sendCommands(
                            listOf(
                                "reselect site",
                                "show me basic info",
                                "show me flooding trend",
                                "show me historical energy demand",
                                "show me forecast energy demand"
                            )
                        )
                    }
                } else if (currentStage == "menu") {
                    when (command.lowercase()) {
                        "reselect site" -> {
                            selectedMineReference = null
                            selectedMineInfo = null
                            currentStage = "selectSite"
                            val siteLabels = sites.map { it.first.lowercase() }
                            sendCommands(siteLabels)
                        }
                        "show me basic info" -> {
                            currentStage = "basicInfo"
                            sendCommands(listOf("back", "menu"))
                        }
                        "show me flooding trend" -> {
                            currentStage = "floodTrend"
                            sendCommands(listOf("back", "menu"))
                        }
                        "show me historical energy demand" -> {
                            currentStage = "historicalEnergy"
                            sendCommands(listOf("back", "menu"))
                        }
                        "show me forecast energy demand" -> {
                            currentStage = "forecastEnergy"
                            sendCommands(listOf("back", "menu"))
                        }
                    }
                } else {
                    if (command.lowercase() in listOf("back", "menu")) {
                        currentStage = "menu"
                        sendCommands(
                            listOf(
                                "reselect site",
                                "show me basic info",
                                "show me flooding trend",
                                "show me historical energy demand",
                                "show me forecast energy demand"
                            )
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                WearMainScreen(
                    stage = currentStage,
                    mineName = selectedMineReference,
                    mineInfo = selectedMineInfo
                )

                AndroidView(
                    factory = { remoteRenderer },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(160.dp)
                        .padding(8.dp)
                )

                AndroidView(
                    factory = { localRenderer },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(80.dp)
                        .padding(8.dp)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.realwear.wearhf.intent.action.SPEECH_EVENT")
        registerReceiver(asrReceiver, filter, Context.RECEIVER_EXPORTED)

        val sites = getAllSiteLabelsReferencesAndNames(this)
        val siteLabels = sites.map { it.first.lowercase() }
        Handler(Looper.getMainLooper()).postDelayed({ sendCommands(siteLabels) }, 100)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(asrReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        eglBase.release()
    }
}


@Composable
fun WearMainScreen(stage: String, mineName: String?, mineInfo: Mine? = null) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (stage) {
                    "selectSite" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionHeader(
                                iconResId = R.drawable.siteinfo_pinandnote, // 請換成適合 icon
                                title = "Select Site",
                                fontSize = 27.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                "Say your site number",
                                color = Color.Black,
                                fontSize = 24.sp
                            )
                        }
                    }
                    "menu" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SectionHeader(
                                iconResId = R.drawable.siteinfo_pinandnote,
                                title = "Menu",
                                fontSize = 27.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Text("Mine: $mineName", color = Color.Black, fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Say a command:", color = Color.Black, fontSize = 21.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("1. Reselect site", color = Color.DarkGray, fontSize = 21.sp)
                            Text("2. Show me basic info", color = Color.DarkGray, fontSize = 21.sp)
                            Text("3. Show me flooding trend", color = Color.DarkGray, fontSize = 21.sp)
                            Text("4. Show me historical energy demand", color = Color.DarkGray, fontSize = 21.sp)
                            Text("5. Show me forecast energy demand", color = Color.DarkGray, fontSize = 21.sp)
                        }
                    }
                    "basicInfo" -> {
                        mineInfo?.let { mine ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SectionHeader(
                                    iconResId = R.drawable.siteinfo_pinandnote,
                                    title = "Basic Information",
                                    fontSize = 27.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    mine.name,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C0471)
                                )
                                Text(
                                    "Reference: ${mine.reference}",
                                    fontSize = 21.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Row {
                                    when (mine.status) {
                                        "C" -> TypeTag("Closed Mine")
                                        "I" -> TypeTag("Inactive Mine")
                                        else -> TypeTag("Status Unknown")
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    "Local Authority: ${mine.localAuthority ?: "Unknown"}",
                                    fontSize = 21.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "Coordinates: E=${mine.easting}, N=${mine.northing}",
                                    fontSize = 21.sp,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Say 'back' or 'menu' to return to menu",
                                    color = Color.DarkGray,
                                    fontSize = 18.sp
                                )
                            }
                        } ?: run {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Loading mine info...", color = Color.Black, fontSize = 21.sp)
                            }
                        }
                    }
                    "floodTrend" -> {
                        mineInfo?.let { mine ->
                            val floodColor = when (mine.floodRiskLevel?.lowercase()) {
                                "high" -> Color.Red
                                "medium" -> Color.Yellow
                                "low" -> Color.Green
                                else -> Color.Gray
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val floodColor = when (mine.floodRiskLevel?.lowercase()) {
                                    "low" -> Color(0xFF00C853)
                                    "medium" -> Color(0xFFFFD600)
                                    "high" -> Color(0xFFD50000)
                                    else -> Color.Gray
                                }
                                SectionHeader(
                                    iconResId = R.drawable.siteinfo_floodingrisks,
                                    title = "Flooding Risks",
                                    trailingContent = {
                                        FloodRiskTag(
                                            label = mine.floodRiskLevel?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                                            color = floodColor
                                        )
                                    }
                                )

                                mine.floodHistory?.let {
                                    FloodHistoryChartMP("Historical Flood Trend Graph", it)
                                } ?: androidx.compose.material3.Text(
                                    "No flood history available",
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Say 'back' or 'menu' to return",
                                    color = Color.DarkGray,
                                    fontSize = 18.sp
                                )
                            }
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading mine info...", color = Color.Gray, fontSize = 21.sp)
                        }
                    }
                    "historicalEnergy" -> {
                        mineInfo?.let { mine ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SectionHeader(
                                    iconResId = R.drawable.siteinfo_energydemand,
                                    title = "Energy Demand",
                                    trailingContent = {
                                        mine.trend?.let { trend ->
                                            val trendColor = when (trend) {
                                                Trend.INCREASING -> Color.Red
                                                Trend.DECREASING -> Color(0xFF00C853)
                                                Trend.STABLE -> Color.Gray
                                            }
                                            val trendLabel = when (trend) {
                                                Trend.INCREASING -> "Increasing"
                                                Trend.DECREASING -> "Decreasing"
                                                Trend.STABLE -> "Stable"
                                            }
                                            TrendTag(label = trendLabel, color = trendColor)
                                        }
                                    }
                                )

                                mine.energyDemandHistory?.let {
                                    EnergyLineChartMP("Historical Energy Demand Graph", it, null)
                                } ?: androidx.compose.material3.Text(
                                    "No energy history available",
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Say 'back' or 'menu' to return",
                                    color = Color.DarkGray,
                                    fontSize = 18.sp
                                )
                            }
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading mine info...", color = Color.Gray, fontSize = 21.sp)
                        }
                    }
                    "forecastEnergy" -> {
                        mineInfo?.let { mine ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SectionHeader(
                                    iconResId = R.drawable.siteinfo_energydemand,
                                    title = "Energy Demand",
                                    trailingContent = {
                                        mine.trend?.let { trend ->
                                            val trendColor = when (trend) {
                                                Trend.INCREASING -> Color.Red
                                                Trend.DECREASING -> Color(0xFF00C853)
                                                Trend.STABLE -> Color.Gray
                                            }
                                            val trendLabel = when (trend) {
                                                Trend.INCREASING -> "Increasing"
                                                Trend.DECREASING -> "Decreasing"
                                                Trend.STABLE -> "Stable"
                                            }
                                            TrendTag(label = trendLabel, color = trendColor)
                                        }
                                    }
                                )

                                mine.forecastEnergyDemand?.let {
                                    EnergyLineChartMP("Forecast Energy Demand Graph", it, null)
                                } ?: Text("No forecast data", color = Color.Gray, fontSize = 21.sp)

                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Say 'back' or 'menu' to return",
                                    color = Color.DarkGray,
                                    fontSize = 18.sp
                                )
                            }
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading mine info...", color = Color.Gray, fontSize = 21.sp)
                        }
                    }
                }
            }
        }
    }
}


