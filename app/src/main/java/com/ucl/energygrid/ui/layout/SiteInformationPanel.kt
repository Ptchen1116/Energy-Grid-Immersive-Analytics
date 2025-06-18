package com.ucl.energygrid.ui.layout

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


@Composable
fun SiteInformationPanel() {
    var note by remember { mutableStateOf("High Likelihood for Gravity Storage") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Site Name", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C0471))
            Text("Location: 54.0°N, 2.3°W", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TypeTag("Closed Mine")
                TypeTag("Closing Mine")
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("📍 Pin & Notes", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { /* TODO: edit logic */ }) {
                    Text("Edit Note")
                }
            }

            SectionHeader("🌊 Flooding Risks")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RiskTag("Low", Color(0xFF00C853))
                RiskTag("Medium", Color(0xFFFFD600))
                RiskTag("High", Color(0xFFD50000))
            }

            GraphSection("Historical Flood Trend Graph")

            SectionHeader("⚡ Energy Demand")
            GraphSection("Historical Energy Demand Graph")

            GraphSection("Forecast Energy Demand Graph", graphColor = Color(0xFFFFC107))

            Button(
                onClick = { /* Run Forecast */ },
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(0.5f)
            ) {
                Text("Run Forecast")
            }

            SectionHeader("📞 Contact Onsite Operator")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.uk_map), // 替換為實際圖片資源
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
fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
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