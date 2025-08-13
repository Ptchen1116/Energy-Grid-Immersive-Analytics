package com.ucl.energygrid.ui.screen

import android.app.Application
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.ucl.energygrid.data.model.BottomSheetContent
import com.ucl.energygrid.data.model.PinType
import com.ucl.energygrid.data.model.RenewableSite
import com.ucl.energygrid.data.remote.apis.RetrofitInstance
import com.ucl.energygrid.ui.layout.bottomNavigationBar.BottomNavigationBar
import com.ucl.energygrid.ui.layout.mapControlPanel.MapControlPanel
import com.ucl.energygrid.ui.layout.siteInformationPanel.SiteInformationPanel
import com.ucl.energygrid.ui.layout.timeSimulationPanel.TimeSimulationPanel
import com.ucl.energygrid.ui.layout.ukMap.UKMap
import com.ucl.energygrid.ui.layout.ukMap.UKMapViewModel
import kotlinx.coroutines.launch
import com.ucl.energygrid.data.repository.UserRepository
import com.ucl.energygrid.data.model.Mine


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userRepository: UserRepository,
)  {
    val context = LocalContext.current.applicationContext as Application
    val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(context))
    val ukMapViewModel: UKMapViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(userRepository)
    )

    val currentBottomSheet by mainViewModel.currentBottomSheet.collectAsState()
    val closedMine by mainViewModel.closedMine.collectAsState()
    val closingMine by mainViewModel.closingMine.collectAsState()
    val selectedMine by mainViewModel.selectedMine.collectAsState()

    val showFloodRisk by mainViewModel.showFloodRisk.collectAsState()
    val floodCenters by mainViewModel.floodCenters.collectAsState()
    val showSolar by mainViewModel.showSolar.collectAsState()
    val showWind by mainViewModel.showWind.collectAsState()
    val showHydroelectric by mainViewModel.showHydroelectric.collectAsState()
    val energyDemandHeatmap by mainViewModel.energyDemandHeatmap.collectAsState()
    val selectedYear by mainViewModel.selectedYear.collectAsState()

    val solarSites by mainViewModel.solarSites.collectAsState()
    val windSites by mainViewModel.windSites.collectAsState()
    val hydroelectricSites by mainViewModel.hydroelectricSites.collectAsState()
    val regionFeatures by mainViewModel.regionFeatures.collectAsState()
    val allMines by mainViewModel.allMines.collectAsState()

    val myPins by mainViewModel.myPins.collectAsState()
    val showMyPinsMarkers by mainViewModel.showMyPinsMarkers.collectAsState()

    var showLoginDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val userId by authViewModel.userId.collectAsState()
    var pinsExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val sheetHeightPx = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        authViewModel.uiMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(showFloodRisk, floodCenters.size) {
        if (showFloodRisk && floodCenters.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No current flood risk areas.")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Energy Insight") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFFAAE5F2)
            ),
            actions = {
                UserAccountActions(
                    isLoggedIn = isLoggedIn,
                    expanded = expanded,
                    onExpandChange = { expanded = it },
                    onShowLoginDialog = { showLoginDialog = true },
                    onShowRegisterDialog = { showRegisterDialog = true },
                    userId = userId,
                    mainViewModel = mainViewModel
                )
            }
        )

        if (showLoginDialog) {
            LoginDialog(
                onDismiss = { showLoginDialog = false },
                onLogin = { email, password ->
                    authViewModel.login(email, password)
                    showLoginDialog = false
                }
            )
        }

        if (showRegisterDialog) {
            RegisterDialog(
                onDismiss = { showRegisterDialog = false },
                onRegister = { username, email, password ->
                    authViewModel.register(username, email, password)
                    showRegisterDialog = false
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
                    BottomSheetContentView(
                        currentBottomSheet = currentBottomSheet,
                        selectedMine = selectedMine,
                        userId = userId,
                        sheetHeightPx = sheetHeightPx,
                        mainViewModel = mainViewModel
                    )
                },
                snackbarHost = {}
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val renewableMarkers = buildList {
                        if (showSolar) addAll(solarSites.map {
                            RenewableSite(it.first, LatLng(it.second, it.third), PinType.SOLAR)
                        })
                        if (showWind) addAll(windSites.map {
                            RenewableSite(it.first, LatLng(it.second, it.third), PinType.WIND)
                        })
                        if (showHydroelectric) addAll(hydroelectricSites.map {
                            RenewableSite(it.first, LatLng(it.second, it.third), PinType.HYDROELECTRIC)
                        })
                    }

                    UKMap(
                        viewModel = ukMapViewModel,
                        floodCenters = floodCenters,
                        showMarkers = showFloodRisk,
                        renewableSites = renewableMarkers,
                        regionFeatures = regionFeatures,
                        energyDemandHeatmap = energyDemandHeatmap,
                        year = selectedYear,
                        closedMine = closedMine,
                        closingMine = closingMine,
                        onSiteSelected = { mine ->
                            mainViewModel.onMineSelected(mine)
                            coroutineScope.launch {
                                mainViewModel.onBottomSheetChange(BottomSheetContent.SiteInfo)
                                scaffoldState.bottomSheetState.expand()
                            }
                        },
                        myPins = if (showMyPinsMarkers) myPins else emptyList(),
                        allMines = allMines,
                        onPinSelected = { mine ->
                            mainViewModel.onSelectedMineChange(mine)
                            coroutineScope.launch {
                                mainViewModel.onBottomSheetChange(BottomSheetContent.SiteInfo)
                                scaffoldState.bottomSheetState.expand()
                            }
                        },
                        onShowMyPinsClick = {
                            if (!showMyPinsMarkers) {
                                if (isLoggedIn) {
                                    val userIdInt = userId?.toIntOrNull() ?: return@UKMap
                                    mainViewModel.loadMyPins(userIdInt, isLoggedIn)
                                } else {
                                    showLoginDialog = true
                                }
                            } else {
                                mainViewModel.clearMyPins()
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
                            mainViewModel.onBottomSheetChange(BottomSheetContent.None)
                        } else {
                            val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            mainViewModel.onBottomSheetChange(BottomSheetContent.MapControl)
                            if (!isExpanded) scaffoldState.bottomSheetState.expand()
                        }
                    }
                },
                onSiteInfoClick = {
                    coroutineScope.launch {
                        if (currentBottomSheet == BottomSheetContent.SiteInfo) {
                            mainViewModel.onBottomSheetChange(BottomSheetContent.None)
                        } else {
                            val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            mainViewModel.onBottomSheetChange(BottomSheetContent.SiteInfo)
                            if (!isExpanded) scaffoldState.bottomSheetState.expand()
                        }
                    }
                },
                onSimulationClick = {
                    coroutineScope.launch {
                        if (currentBottomSheet == BottomSheetContent.TimeSimulation) {
                            mainViewModel.onBottomSheetChange(BottomSheetContent.None)
                        } else {
                            val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            mainViewModel.onBottomSheetChange(BottomSheetContent.TimeSimulation)
                            if (!isExpanded) scaffoldState.bottomSheetState.expand()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun UserAccountActions(
    isLoggedIn: Boolean,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onShowLoginDialog: () -> Unit,
    onShowRegisterDialog: () -> Unit,
    userId: String?,
    mainViewModel: MainViewModel,
) {
    Box {
        IconButton(onClick = {
            if (isLoggedIn) {
                onExpandChange(true)
            } else {
                onShowLoginDialog()
            }
        }) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Login")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandChange(false)
            }
        ) {
            DropdownMenuItem(
                text = { Text("My Pins") },
                onClick = {
                    if (isLoggedIn) {
                        val userIdInt = userId?.toIntOrNull() ?: return@DropdownMenuItem
                        mainViewModel.loadMyPins(userIdInt, isLoggedIn)
                    } else {
                        onShowLoginDialog()
                    }
                    onExpandChange(false)
                }
            )
        }
    }

    if (!isLoggedIn) {
        IconButton(onClick = { onShowRegisterDialog() }) {
            Icon(Icons.Default.AddCircle, contentDescription = "Register")
        }
    }
}

@Composable
fun BottomSheetContentView(
    currentBottomSheet: BottomSheetContent,
    selectedMine: Mine?,
    userId: String?,
    sheetHeightPx: androidx.compose.runtime.MutableIntState,
    mainViewModel: MainViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .onGloballyPositioned { coordinates ->
                sheetHeightPx.intValue = coordinates.size.height
            }
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        val userIdInt = userId?.toIntOrNull() ?: -1

        when (currentBottomSheet) {
            BottomSheetContent.SiteInfo -> {
                selectedMine?.let { mine ->
                    SiteInformationPanel(
                        mine = mine,
                        userId = userIdInt,
                        pinApi = RetrofitInstance.pinApi
                    )
                }
            }
            BottomSheetContent.MapControl -> MapControlPanel(
                floodingRisk = mainViewModel.showFloodRisk.collectAsState().value,
                onFloodingRiskChange = { mainViewModel.toggleShowFloodRisk(it) },
                showSolar = mainViewModel.showSolar.collectAsState().value,
                onSolarChange = { mainViewModel.toggleShowSolar(it) },
                showWind = mainViewModel.showWind.collectAsState().value,
                onWindChange = { mainViewModel.toggleShowWind(it) },
                showHydroelectric = mainViewModel.showHydroelectric.collectAsState().value,
                onHydroelectricChange = { mainViewModel.toggleShowHydroelectric(it) },
                closedMine = mainViewModel.closedMine.collectAsState().value,
                onClosedMineChange = { mainViewModel.updateClosedMine(it) },
                closingMine = mainViewModel.closingMine.collectAsState().value,
                onClosingMineChange = { mainViewModel.updateClosingMine(it) },
            )
            BottomSheetContent.TimeSimulation -> TimeSimulationPanel(
                energyDemandHeatmap = mainViewModel.energyDemandHeatmap.collectAsState().value,
                onEnergyDemandHeatmapChange = { mainViewModel.toggleEnergyDemandHeatmap(it) },
                selectedYear = mainViewModel.selectedYear.collectAsState().value,
                onSelectedYearChange = { mainViewModel.changeSelectedYear(it) }
            )
            else -> Spacer(modifier = Modifier.height(0.dp))
        }
        Spacer(modifier = Modifier.height(75.dp))
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

