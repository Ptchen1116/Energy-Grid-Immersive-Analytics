package com.ucl.energygrid.ui.layout.siteInformationPanel

import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.ucl.energygrid.CallingViewModel
import com.ucl.energygrid.R
import com.ucl.energygrid.data.model.EnergyDemand
import com.ucl.energygrid.data.model.FloodEvent
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.Trend
import com.ucl.energygrid.data.remote.apis.PinApi
import com.ucl.energygrid.data.repository.WebRtcRepository
import com.ucl.energygrid.ui.component.TypeTag
import com.ucl.energygrid.ui.screen.MainViewModel
import org.webrtc.SurfaceViewRenderer


@Composable
fun SiteInformationPanel(
    mine: Mine,
    userId: Int,
    pinApi: PinApi,
    viewModel: SiteInformationViewModel = viewModel(
        key = mine.reference.toString(),
        factory = SiteInformationViewModelFactory(userId, mine.reference.toInt(), pinApi)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val mainViewModel: MainViewModel = viewModel()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                mine.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C0471)
            )
            Text(
                "Location: ${mine.northing}°N, ${mine.easting}°E",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.padding(8.dp)) {
                when (mine.status) {
                    "C" -> TypeTag("Closed Mine")
                    "I" -> TypeTag("Inactive Mine")
                }
            }

            SectionHeader(
                iconResId = R.drawable.siteinfo_pinandnote,
                title = "Pin & Note"
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (userId == -1) {
                Text(
                    text = "Login to pin the location",
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = { viewModel.updateNote(it) },
                    label = { Text("Note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = uiState.isNoteLoaded
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveNote() },
                        enabled = !uiState.isPosting && uiState.isNoteLoaded,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                uiState.isPosting -> "Saving..."
                                uiState.note.isNotEmpty() -> "Update Note"
                                else -> "Add Pin"
                            }
                        )
                    }

                    if (uiState.isPinned) {
                        Button(
                            onClick = {
                                viewModel.removePin()
                                mainViewModel.loadMyPins(userId, true)
                            },
                            enabled = !uiState.isPosting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remove Pin")
                        }
                    }
                }

                uiState.postResult?.let {
                    Text(
                        text = it,
                        color = if (it.startsWith("Note saved") || it.startsWith("Pin removed")) Color(0xFF00C853) else Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

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
            } ?: Text("No flood history available", color = Color.Gray)

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
            } ?: Text("No energy history available", color = Color.Gray)

            Spacer(modifier = Modifier.height(30.dp))

            mine.forecastEnergyDemand?.let {
                EnergyLineChartMP("Forecast Energy Demand Graph", it, null)
            } ?: Text("No forecast data available", color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(
                iconResId = R.drawable.siteinfo_call,
                title = "Contact Onsite Operator"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CallingView(isCaller = true)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}


@Composable
fun SectionHeader(
    iconResId: Int? = null,
    title: String,
    trailingContent: (@Composable () -> Unit)? = null,
    fontSize: TextUnit = 18.sp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (iconResId != null) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )

            trailingContent?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }
    }
}

@Composable
fun TrendTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun FloodRiskTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}



@Composable
fun EnergyLineChartMP(title: String, data: List<EnergyDemand>, trend: Trend?) {
    if (data.isEmpty()) {
        Text("No data available", color = Color.Gray)
        return
    }

    val trendColor = when (trend) {
        Trend.INCREASING -> android.graphics.Color.RED
        Trend.DECREASING -> android.graphics.Color.GREEN
        Trend.STABLE -> android.graphics.Color.GRAY
        null -> android.graphics.Color.GRAY
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            trend?.let {
                TrendTag(
                    label = when (it) {
                        Trend.INCREASING -> "Increasing"
                        Trend.DECREASING -> "Decreasing"
                        Trend.STABLE -> "Stable"
                    },
                    color = when (it) {
                        Trend.INCREASING -> Color.Red
                        Trend.DECREASING -> Color(0xFF00C853)
                        Trend.STABLE -> Color.Gray
                    }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            factory = { context ->
                LineChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.WHITE)
                    description.isEnabled = false
                    axisRight.isEnabled = false
                    legend.isEnabled = false
                }
            },
            update = { chart ->
                val entries = data.mapIndexed { i, it ->
                    Entry(i.toFloat(), it.value.toFloat())
                }
                val lineDataSet = LineDataSet(entries, "Energy Demand").apply {
                    color = trendColor
                    valueTextColor = android.graphics.Color.BLACK
                    setDrawCircles(true)
                    lineWidth = 3f
                    circleRadius = 6f
                    setDrawValues(false)
                }

                chart.data = LineData(lineDataSet)

                chart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(data.map { it.year.toString() })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                }

                chart.axisLeft.apply {
                    axisMinimum = (data.minOf { it.value } * 0.9f).toFloat()
                    axisMaximum = (data.maxOf { it.value } * 1.1f).toFloat()
                }
                chart.invalidate()
            }
        )
    }
}

@Composable
fun FloodHistoryChartMP(title: String, data: List<FloodEvent>) {
    if (data.isEmpty()) {
        Text("No data available", color = Color.Gray)
        return
    }

    val floodColor = android.graphics.Color.parseColor("#1E88E5")

    val yearsRange = (2015..2023).toList()
    val completeData = yearsRange.map { year ->
        data.find { it.year == year } ?: FloodEvent(year, 0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        AndroidView(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
            factory = { context ->
                LineChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.WHITE)
                    description.isEnabled = false
                    axisRight.isEnabled = false
                    legend.isEnabled = false
                }
            },
            update = { chart ->
                val entries = completeData.mapIndexed { i, it ->
                    Entry(i.toFloat(), it.events.toFloat())
                }
                val lineDataSet = LineDataSet(entries, "Flood Events").apply {
                    color = floodColor
                    valueTextColor = android.graphics.Color.BLACK
                    setDrawCircles(true)
                    lineWidth = 3f
                    circleRadius = 6f
                    setDrawValues(false)
                }

                chart.data = LineData(lineDataSet)

                chart.xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(completeData.map { it.year.toString() })
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                }

                val maxEvents = completeData.maxOf { it.events }

                chart.axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = maxEvents.toFloat()
                    granularity = 1f
                    setLabelCount(maxEvents + 1, true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return value.toInt().toString()
                        }
                    }
                }

                chart.invalidate()
            }
        )
    }
}

@Composable
fun CallingView(isCaller: Boolean) {
    val context = LocalContext.current

    val callingViewModel: CallingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repo = WebRtcRepository(context)
                @Suppress("UNCHECKED_CAST")
                return CallingViewModel(repo) as T
            }
        }
    )

    val localView = remember { SurfaceViewRenderer(context) }
    val remoteView = remember { SurfaceViewRenderer(context) }

    val isConnected by callingViewModel.isConnected.collectAsState()

    LaunchedEffect(localView, remoteView) {
        callingViewModel.init(localView, remoteView, isCaller)
    }

    DisposableEffect(Unit) {
        onDispose {
            callingViewModel.endCall()
            localView.release()
            remoteView.release()
        }
    }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AndroidView(
            factory = { remoteView },
            modifier = Modifier
                .width(300.dp)
                .height(300.dp)
        )

        Button(onClick = {
            if (!isConnected) {
                callingViewModel.startCall()
            } else {
                callingViewModel.endCall()
            }
        }) {
            Text(if (!isConnected) "Start Call" else "End Call")
        }
    }
}
