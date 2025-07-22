package com.ucl.energygrid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Text
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.ucl.energygrid.data.getAllSiteLabelsReferencesAndNames
import com.ucl.energygrid.data.getInfoByReference
import com.ucl.energygrid.ui.screen.Mine
import androidx.compose.ui.platform.LocalContext

class WearMainActivity : ComponentActivity() {
    private val _spokenCommand = MutableStateFlow("No selection")
    val spokenCommand: StateFlow<String> get() = _spokenCommand



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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContent {
            val command by spokenCommand.collectAsState()
            val context = LocalContext.current
            val sites = remember { getAllSiteLabelsReferencesAndNames(context) }

            var currentStage by remember { mutableStateOf("selectSite") }
            var selectedMine by remember { mutableStateOf<String?>(null) }
            var selectedMineReference by remember { mutableStateOf<String?>(null) }
            var selectedMineInfo by remember { mutableStateOf<Mine?>(null) }

            LaunchedEffect(command) {
                Log.i("WearMainCommand", "Received command: '$command'")
                if (currentStage == "selectSite") {
                    val selectedSite = sites.firstOrNull {
                        it.first.equals(command, ignoreCase = true) ||
                                command in listOf("one", "1", "two", "2", "three", "3") && it.first.endsWith(command.takeLast(1))
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

            WearMainScreen(
                stage = currentStage,
                mineName = selectedMineReference,
                mineInfo = selectedMineInfo
            )
        }
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
                        Text("Say your site number", color = Color.Black)
                    }
                    "menu" -> {
                        Text("Mine: $mineName", color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Say a command:", color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. Reselect site", color = Color.DarkGray)
                        Text("2. Show me basic info", color = Color.DarkGray)
                        Text("3. Show me flooding trend", color = Color.DarkGray)
                        Text("4. Show me historical energy demand", color = Color.DarkGray)
                        Text("5. Show me forecast energy demand", color = Color.DarkGray)
                    }
                    "basicInfo" -> {
                        mineInfo?.let { mine ->
                            Text(
                                "Basic Information",
                                color = Color.Black,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Name: ${mine.name}", color = Color.Black)
                            Text("Reference: ${mine.reference}", color = Color.Black)
                            Text("Status: ${mine.status ?: "Unknown"}", color = Color.Black)
                            Text(
                                "Local Authority: ${mine.localAuthority ?: "Unknown"}",
                                color = Color.Black
                            )
                            Text(
                                "Coordinates: E=${mine.easting}, N=${mine.northing}",
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Say 'back' or 'menu' to return to menu", color = Color.DarkGray)
                        } ?: run {
                            Text("Loading mine info...", color = Color.Black)
                        }
                    }
                    "floodTrend" -> {
                        mineInfo?.let { mine ->
                            Text("Flood Risk Level: ${mine.floodRiskLevel}", color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        mineInfo?.floodHistory?.let {
                            it.forEach { event ->
                                Text("Year ${event.year}: ${event.events} events", color = Color.DarkGray)
                            }
                        } ?: Text("No flood history available", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Say 'back' or 'menu' to return", color = Color.DarkGray)
                    }
                    "historicalEnergy" -> {
                        Text("Historical Energy Demand", color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        mineInfo?.energyDemandHistory?.let {
                            it.forEach { demand ->
                                Text("${demand.year}: ${demand.value} kWh", color = Color.DarkGray)
                            }
                        } ?: Text("No historical data", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Say 'back' or 'menu' to return", color = Color.DarkGray)
                    }
                    "forecastEnergy" -> {
                        Text("Forecast Energy Demand", color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        mineInfo?.forecastEnergyDemand?.let {
                            it.forEach { demand ->
                                Text("${demand.year}: ${demand.value} kWh", color = Color.DarkGray)
                            }
                        } ?: Text("No forecast data", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Say 'back' or 'menu' to return", color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

