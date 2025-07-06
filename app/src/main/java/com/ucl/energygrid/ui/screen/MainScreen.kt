package com.ucl.energygrid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.ucl.energygrid.data.GeoJsonLoader
import com.ucl.energygrid.data.RegionFeature
import com.ucl.energygrid.data.fetchAllFloodCenters
import com.ucl.energygrid.data.readAndExtractSitesByType
import com.ucl.energygrid.ui.component.PinType
import com.ucl.energygrid.ui.layout.BottomNavigationBar
import com.ucl.energygrid.ui.layout.MapControlPanel
import com.ucl.energygrid.ui.layout.SiteInformationPanel
import com.ucl.energygrid.ui.layout.TimeSimulationPanel
import kotlinx.coroutines.launch
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import android.widget.Toast
import com.ucl.energygrid.data.API.RetrofitInstance
import com.ucl.energygrid.data.API.RegisterRequest
import com.ucl.energygrid.data.API.LoginRequest

enum class BottomSheetContent {
    None,
    SiteInfo,
    MapControl,
    TimeSimulation
}

data class RenewableSite(
    val name: String,
    val location: LatLng,
    val type: PinType
)

data class EnergyDemand(
    val year: Int,
    val value: Double
)

data class Mine(
    val reference: String,
    val name: String,
    val status: String,
    val easting: Double,
    val northing: Double,
    val localAuthority: String?,
    val note: String?,
    val floodRiskLevel: String?,
    val floodHistory: String?,
    val energyDemandHistory: List<EnergyDemand>?,
    val forecastEnergyDemand: Any?
)

data class YearValue(val year: Int, val value: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentBottomSheet by remember { mutableStateOf(BottomSheetContent.None) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val sheetHeightPx = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    var closedMine by remember { mutableStateOf(false) }
    var closingMine by remember { mutableStateOf(false) }

    var selectedMine by remember { mutableStateOf<Mine?>(null) }


    val context = LocalContext.current
    var showFloodRisk by remember { mutableStateOf(false) }
    val floodCenters = remember { mutableStateListOf<LatLng>() }
    val snackbarHostState = remember { SnackbarHostState() }

    var showSolar by remember { mutableStateOf(false) }
    var showWind by remember { mutableStateOf(false) }
    var showHydroelectric by remember { mutableStateOf(false) }

    val solarSites = readAndExtractSitesByType(context, category = "solar")
    val windSites = readAndExtractSitesByType(context, category = "wind")
    val hydroelectricSites = readAndExtractSitesByType(context, category = "hydroelectric")

    val regionFeatures = remember { mutableStateListOf<RegionFeature>() }
    var energyDemandHeatmap by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf(2025) }

    var showLoginDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val fetchedCenters = fetchAllFloodCenters(context)
        floodCenters.clear()
        floodCenters.addAll(fetchedCenters)
    }

    LaunchedEffect(showFloodRisk, floodCenters.size) {
        if (showFloodRisk && floodCenters.isEmpty()) {
            snackbarHostState.showSnackbar("No current flood risk areas.")
        }
    }


    LaunchedEffect(Unit) {
        GeoJsonLoader.loadGeoJsonFeatures { features ->
            regionFeatures.clear()
            regionFeatures.addAll(features)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Energy Insight") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFFAAE5F2)
            ),
            actions = {
                IconButton(onClick = { showLoginDialog = true }) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Login")
                }
                IconButton(onClick = { showRegisterDialog = true }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Register")
                }
            }
        )

        if (showLoginDialog) {
            LoginDialog(
                onDismiss = { showLoginDialog = false },
                onLogin = { email, password ->
                    coroutineScope.launch {
                        try {
                            val api = RetrofitInstance.userApi
                            val response = api.login(LoginRequest(email, password))
                            if (response.isSuccessful) {
                                val loginResponse = response.body()
                                val token = loginResponse?.access_token
                                Toast.makeText(context, "Login success! Token: $token", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Login failed: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (showRegisterDialog) {
            RegisterDialog(
                onDismiss = { showRegisterDialog = false },
                onRegister = { username, email, password ->
                    coroutineScope.launch {
                        try {
                            val api = RetrofitInstance.userApi
                            val response = api.register(RegisterRequest(username, email, password))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Register successful!", Toast.LENGTH_SHORT).show()
                                showRegisterDialog = false
                            } else {
                                Toast.makeText(context, "Register failed: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Register error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = 0.dp,
                sheetSwipeEnabled = true,
                sheetDragHandle = null,
                sheetContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .onGloballyPositioned { coordinates ->
                                sheetHeightPx.intValue = coordinates.size.height
                            }
                            .padding(
                                bottom = WindowInsets.navigationBars
                                    .asPaddingValues()
                                    .calculateBottomPadding()
                            )
                    ) {
                        when (currentBottomSheet) {
                            BottomSheetContent.SiteInfo -> {
                                selectedMine?.let {
                                    SiteInformationPanel(mine = it)
                                }
                            }
                            BottomSheetContent.MapControl -> MapControlPanel(
                                floodingRisk = showFloodRisk,
                                onFloodingRiskChange = { showFloodRisk = it },
                                showSolar = showSolar,
                                onSolarChange = { showSolar = it },
                                showWind = showWind,
                                onWindChange = { showWind = it },
                                showHydroelectric = showHydroelectric,
                                onHydroelectricChange = { showHydroelectric = it },
                                closedMine = closedMine,
                                onClosedMineChange = { closedMine = it },
                                closingMine = closingMine,
                                onClosingMineChange = { closingMine = it }
                            )
                            BottomSheetContent.TimeSimulation -> TimeSimulationPanel(
                                energyDemandHeatmap = energyDemandHeatmap,
                                onEnergyDemandHeatmapChange = { energyDemandHeatmap = it },
                                selectedYear = selectedYear,
                                onSelectedYearChange = { selectedYear = it }
                            )
                            else -> Spacer(modifier = Modifier.height(0.dp))
                        }
                        Spacer(modifier = Modifier.height(75.dp))
                    }
                },
                snackbarHost = {}
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val renewableMarkers = mutableListOf<RenewableSite>()
                    if (showSolar) {
                        renewableMarkers.addAll(
                            solarSites.map {
                                RenewableSite(
                                    name = it.first,
                                    location = LatLng(it.second, it.third),
                                    type = PinType.SOLAR
                                )
                            }
                        )
                    }
                    if (showWind) {
                        renewableMarkers.addAll(
                            windSites.map {
                                RenewableSite(
                                    name = it.first,
                                    location = LatLng(it.second, it.third),
                                    type = PinType.WIND
                                )
                            }
                        )
                    }
                    if (showHydroelectric) {
                        renewableMarkers.addAll(
                            hydroelectricSites.map {
                                RenewableSite(
                                    name = it.first,
                                    location = LatLng(it.second, it.third),
                                    type = PinType.HYDROELECTRIC
                                )
                            }
                        )
                    }

                    UKMap(
                        floodCenters = floodCenters,
                        showMarkers = showFloodRisk,
                        renewableSites = renewableMarkers,
                        regionFeatures = regionFeatures,
                        energyDemandHeatmap = energyDemandHeatmap,
                        year = selectedYear,
                        closedMine = closedMine,
                        closingMine = closingMine,
                        markerIcons = emptyMap(),
                        onSiteSelected = { mine ->
                            selectedMine = mine
                            coroutineScope.launch {
                                currentBottomSheet = BottomSheetContent.SiteInfo
                                scaffoldState.bottomSheetState.expand()
                            }
                        }
                    )

                    if (currentBottomSheet != BottomSheetContent.None) {
                        val overlayHeightDp = with(density) {
                            (sheetHeightPx.intValue + 50).toDp()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(overlayHeightDp)
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x99000000)
                                        ),
                                        startY = 0f,
                                        endY = 300f
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp
                                    )
                                )
                        )
                    }
                }
            }

            val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = screenHeightDp * 0.15f)
                    .align(Alignment.TopCenter)
            ) {
                SnackbarHost(hostState = snackbarHostState)
            }

            BottomNavigationBar(
                selectedItem = currentBottomSheet,
                modifier = Modifier.align(Alignment.BottomCenter),
                onMapControlClick = {
                    coroutineScope.launch {
                        if (currentBottomSheet == BottomSheetContent.MapControl) {
                            currentBottomSheet = BottomSheetContent.None
                        } else {
                            val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            currentBottomSheet = BottomSheetContent.MapControl
                            if (!isExpanded) scaffoldState.bottomSheetState.expand()
                        }
                    }
                },
                onSiteInfoClick = {
                    coroutineScope.launch {
                        if (currentBottomSheet == BottomSheetContent.SiteInfo) {
                            currentBottomSheet = BottomSheetContent.None
                        } else {
                            val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            currentBottomSheet = BottomSheetContent.SiteInfo
                            if (!isExpanded) scaffoldState.bottomSheetState.expand()
                        }
                    }
                },
                onSimulationClick = {
                    coroutineScope.launch {
                        if (currentBottomSheet == BottomSheetContent.TimeSimulation) {
                            currentBottomSheet = BottomSheetContent.None
                        } else {
                            val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            currentBottomSheet = BottomSheetContent.TimeSimulation
                            if (!isExpanded) scaffoldState.bottomSheetState.expand()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login") },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onLogin(email, password)
                    onDismiss()
                }
            ) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RegisterDialog(
    onDismiss: () -> Unit,
    onRegister: (username: String, email: String, password: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") }
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRegister(username, email, password)
                    onDismiss()
                }
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}