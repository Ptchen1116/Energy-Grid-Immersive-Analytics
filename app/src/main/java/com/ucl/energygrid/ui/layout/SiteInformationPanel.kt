package com.ucl.energygrid.ui.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ucl.energygrid.R
import com.ucl.energygrid.ui.component.RiskTag
import com.ucl.energygrid.ui.component.TypeTag
import com.ucl.energygrid.ui.screen.Mine
import com.ucl.energygrid.ui.screen.EnergyDemand
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.width
import com.ucl.energygrid.data.API.RetrofitInstance
import com.ucl.energygrid.data.API.PinRequest
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.ucl.energygrid.data.API.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SiteInformationPanel(mine: Mine, userId: Int) {
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn

    var note by remember { mutableStateOf(mine.note ?: "") }
    var isPosting by remember { mutableStateOf(false) }
    var postResult by remember { mutableStateOf<String?>(null) }
    var isNoteLoaded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitInstance.pinApi.getPin(userId, mine.reference.toInt())
            if (response.isSuccessful) {
                note = response.body()?.note ?: ""
            } else {
                postResult = "Failed to load note: ${response.code()}"
            }
        } catch (e: Exception) {
            postResult = "Error loading note: ${e.message}"
        } finally {
            isNoteLoaded = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(mine.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C0471))
            Text("Location: ${mine.northing}°N, ${mine.easting}°E", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.padding(8.dp)) {
                when (mine.status) {
                    "C" -> TypeTag("Closed Mine")
                    "I" -> TypeTag("Inactive Mine")
                    else -> { /* No Tag */ }
                }
            }

            SectionHeader(
                iconResId = R.drawable.siteinfo_pinandnote,
                title = "Note"
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isLoggedIn) {
                Text(
                    text = "Login to pin the location",
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = isNoteLoaded
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isPosting = true
                        postResult = null
                        coroutineScope.launch {
                            try {
                                val response = RetrofitInstance.pinApi.create_or_update_pin(
                                    userId = userId,
                                    pinRequest = PinRequest(mine_id = mine.reference.toInt(), note = note)
                                )
                                if (response.isSuccessful) {
                                    postResult = "Note saved successfully"
                                } else {
                                    postResult = "Failed: ${response.code()}"
                                }
                            } catch (e: Exception) {
                                postResult = "Error: ${e.message}"
                            } finally {
                                isPosting = false
                            }
                        }
                    },
                    enabled = !isPosting && isNoteLoaded
                ) {
                    Text(if (isPosting) "Saving..." else if (note.isNotEmpty()) "Update Note" else "Add Note")
                }

                postResult?.let {
                    Text(
                        text = it,
                        color = if (it.startsWith("Note saved") || it.startsWith("Note updated")) Color.Green else Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }


            SectionHeader(
                iconResId = R.drawable.siteinfo_floodingrisks,
                title = "Flooding Risks"
            )

            val floodColor = when (mine.floodRiskLevel?.lowercase()) {
                "low" -> Color(0xFF00C853)
                "medium" -> Color(0xFFFFD600)
                "high" -> Color(0xFFD50000)
                else -> Color.Gray
            }
            RiskTag(mine.floodRiskLevel ?: "Unknown", floodColor)

            GraphSection("Historical Flood Trend Graph")

            SectionHeader(
                iconResId = R.drawable.siteinfo_energydemand,
                title = "Energy Demand"
            )
            mine.energyDemandHistory?.let {
                EnergyLineChart("Historical Energy Demand Graph",it)
            } ?: Text("No energy history available", color = Color.Gray)

            GraphSection("Forecast Energy Demand Graph", graphColor = Color(0xFFFFC107))

            Button(
                onClick = { /* Run Forecast */ },
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(0.5f)
            ) {
                Text("Run Forecast")
            }

            SectionHeader(
                iconResId = R.drawable.siteinfo_call,
                title = "Contact Onsite Operator"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.uk_map),
                    contentDescription = "Operator",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Text("Cassie Jung", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                Button(onClick = { /* End Call */ }) {
                    Text("End Call")
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SectionHeader(iconResId: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GraphSection(title: String, graphColor: Color = Color.Blue) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth()
                .background(graphColor.copy(alpha = 0.2f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Graph Placeholder", color = graphColor)
        }
    }
}

@Composable
fun EnergyLineChart(title: String, data: List<EnergyDemand>) {
    if (data.isEmpty()) {
        Text("No data available", color = Color.Gray)
        return
    }

    val maxVal = data.maxOf { it.value }.toFloat()
    val minVal = data.minOf { it.value }.toFloat()
    val yearLabels = data.map { it.year.toString() }

    val stroke = Stroke(width = 4f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spaceX = size.width / (data.size - 1).coerceAtLeast(1)
                val spaceY = size.height

                val points = data.mapIndexed { index, entry ->
                    val x = index * spaceX
                    val y = spaceY - (((entry.value.toFloat() - minVal) / (maxVal - minVal)) * spaceY)
                    Offset(x, y)
                }

                // Draw line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color.Blue,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = stroke.width
                    )
                }

                // Draw points
                points.forEach {
                    drawCircle(Color.Blue, radius = 6f, center = it)
                }
            }

            // Year labels below the graph
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    yearLabels.forEach {
                        Text(it, fontSize = 10.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}