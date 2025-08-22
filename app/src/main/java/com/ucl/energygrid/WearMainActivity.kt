package com.ucl.energygrid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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

    companion object {
        private const val CAMERA_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }
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
            val sitesState = produceState<List<Triple<String, String, String>>>(initialValue = emptyList()) {
                value = getAllSiteLabelsReferencesAndNames()
            }
            val sites = sitesState.value
            val incomingCall by viewModel.incomingCall.collectAsState()
            var showIncomingDialog by remember { mutableStateOf(false) }
            val callActive by viewModel.callActive.collectAsState()

            LaunchedEffect(incomingCall) {
                showIncomingDialog = incomingCall
            }

            if (showIncomingDialog) {
                IncomingCallDialog(
                    callerName = "Incoming Call",
                    onAccept = {
                        viewModel.acceptCall()
                        showIncomingDialog = false
                    },
                    onReject = {
                        viewModel.rejectCall()
                        showIncomingDialog = false
                    }
                )
            }

            val commandHandler = remember(sites) {
                CommandHandler(
                    sites = sites,
                    sitesPerPage = 5,
                    getInfoByReference = { ref -> getInfoByReference(ref) }
                )
            }

            LaunchedEffect(command) {
                if (sites.isEmpty()) return@LaunchedEffect
                val result = commandHandler.handleCommand(command, viewModel)
                sendCommands(result.sendCommands)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val commandHandler = remember(sites) {
                    CommandHandler(
                        sites = sites,
                        sitesPerPage = 5,
                        getInfoByReference = { ref -> getInfoByReference(ref) }
                    )
                }

                val scope = rememberCoroutineScope()

                LaunchedEffect(command) {
                    val result = commandHandler.handleCommand(command, viewModel)
                    sendCommands(result.sendCommands)
                }

                WearMainScreen(
                    stage = commandHandler.currentStage,
                    mineName = commandHandler.selectedMineName,
                    mineInfo = commandHandler.selectedMineInfo,
                    sites = sites.drop(commandHandler.currentPage * 5).take(5),
                    onMenuClick = {
                        scope.launch {
                            val result = commandHandler.handleCommand("menu", viewModel)
                            sendCommands(result.sendCommands)
                        }
                    },
                    onNextClick = {
                        scope.launch {
                            val result = commandHandler.handleCommand("next", viewModel)
                            sendCommands(result.sendCommands)
                        }
                    },
                    onPreviousClick = {
                        scope.launch {
                            val result = commandHandler.handleCommand("previous", viewModel)
                            sendCommands(result.sendCommands)
                        }
                    }
                )

                if (callActive) {
                    AndroidView(
                        factory = { localRenderer },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(160.dp)
                            .padding(end = 10.dp, bottom = 85.dp)
                    )
                    DisposableEffect(Unit) {
                        onDispose {
                            localRenderer.clearImage()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.realwear.wearhf.intent.action.SPEECH_EVENT")
        registerReceiver(asrReceiver, filter, Context.RECEIVER_EXPORTED)

        lifecycleScope.launch {
            val sites = getAllSiteLabelsReferencesAndNames()
            val siteLabels = sites.map { it.first.lowercase() }
            withContext(Dispatchers.Main) {
                val callActions = listOf("Accept", "Reject")
                val changePages = listOf("Next", "Previous")
                sendCommands(siteLabels + callActions + changePages)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(asrReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        localRenderer.release()
        remoteRenderer.release()
        eglBase.release()
    }
}


@Composable
fun WearMainScreen(
    stage: String,
    mineName: String?,
    mineInfo: Mine? = null,
    sites: List<Triple<String, String, String>> = emptyList(),
    onMenuClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = Color(0xFFAAE5F2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text("Menu")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (stage) {
                    "selectSite" -> SelectSiteScreen(sites)
                    "basicInfo" -> mineInfo?.let { BasicInfoScreen(it) } ?: LoadingScreen()
                    "floodTrend" -> mineInfo?.let { FloodTrendScreen(it) } ?: LoadingScreen()
                    "historicalEnergy" -> mineInfo?.let { HistoricalEnergyScreen(it) } ?: LoadingScreen()
                    "forecastEnergy" -> mineInfo?.let { ForecastEnergyScreen(it) } ?: LoadingScreen()
                    "menu" -> mineName?.let { MenuScreen(it) } ?: LoadingScreen()
                }
            }

            Surface(
                color = Color(0xFFAAE5F2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = onPreviousClick) {
                            Text("Previous")
                        }
                        Button(onClick = onNextClick) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectSiteScreen(sites: List<Triple<String, String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        SectionHeader(
            title = "Select Site",
            fontSize = 27.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Say your site number", color = Color.Black, fontSize = 20.sp, modifier = Modifier
            .fillMaxWidth()
            .padding(start = 300.dp))
        Spacer(modifier = Modifier.height(12.dp))

        sites.forEach { (label, _, name) ->
            Text(
                text = "$label: $name",
                color = Color.DarkGray,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(start = 300.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("(Say 'next' or 'previous' to change page)", fontSize = 14.sp, color = Color.Gray, modifier = Modifier
            .fillMaxWidth()
            .padding(start = 300.dp))
    }
}

@Composable
fun MenuScreen(mineName: String) {
    val commands = listOf(
        "Reselect Site",
        "Basic Info",
        "Flooding Trend",
        "Historical Energy Demand",
        "Forecast Energy Demand"
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Say 'back' to close the menu", fontSize = 18.sp, color = Color.Black)
        Spacer(Modifier.height(10.dp))

        commands.forEach { cmd ->
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(cmd, fontSize = 18.sp)
            }
        }
    }
}


@Composable
fun BasicInfoScreen(mine: Mine) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(
            title = "Basic Information",
            fontSize = 27.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(mine.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C0471))
        Text("Reference: ${mine.reference}", fontSize = 21.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(18.dp))
        Row {
            when (mine.status) {
                "C" -> TypeTag("Closed Mine")
                "I" -> TypeTag("Inactive Mine")
                else -> TypeTag("Status Unknown")
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("Local Authority: ${mine.localAuthority ?: "Unknown"}", fontSize = 21.sp, color = Color.Black)
        Text("Coordinates: E=${mine.easting}, N=${mine.northing}", fontSize = 21.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Say 'back' or 'menu' to return to menu", color = Color.DarkGray, fontSize = 18.sp)
    }
}

@Composable
fun FloodTrendScreen(mine: Mine) {
    val floodColor = when (mine.floodRiskLevel?.lowercase()) {
        "low" -> Color(0xFF00C853)
        "medium" -> Color(0xFFFFD600)
        "high" -> Color(0xFFD50000)
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(
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
        } ?: Text("No flood history available", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Say 'back' or 'menu' to return", color = Color.DarkGray, fontSize = 18.sp)
    }
}

@Composable
fun HistoricalEnergyScreen(mine: Mine) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(
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
        } ?: Text("No energy history available", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Say 'back' or 'menu' to return", color = Color.DarkGray, fontSize = 18.sp)
    }
}

@Composable
fun ForecastEnergyScreen(mine: Mine) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionHeader(
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
        Text("Say 'back' or 'menu' to return", color = Color.DarkGray, fontSize = 18.sp)
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading mine info...", color = Color.Gray, fontSize = 21.sp)
    }
}


@Composable
fun IncomingCallDialog(
    callerName: String = "Incoming Call",
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onReject() },
        title = { Text(text = callerName, style = MaterialTheme.typography.titleLarge) },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accept")
            }
        },
        dismissButton = {
            Button(onClick = onReject) {
                Text("Reject")
            }
        }
    )
}

fun getCurrentPageCommands(
    sites: List<Triple<String, String, String>>,
    page: Int,
    perPage: Int
): List<String> {
    return sites.drop(page * perPage).take(perPage).map { it.first.lowercase() }
}