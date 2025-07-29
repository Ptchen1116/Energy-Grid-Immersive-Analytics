package com.ucl.energygrid.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ucl.energygrid.data.model.EnergyDemand
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.PinType
import com.ucl.energygrid.data.model.Trend
import com.ucl.energygrid.data.remote.apis.RetrofitInstance
import com.ucl.energygrid.ui.component.createPinBitmap
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

fun convertOSGB36ToWGS84(easting: Double, northing: Double): LatLng {
    val csFactory = CRSFactory()
    val osgb36 = csFactory.createFromName("EPSG:27700")
    val wgs84 = csFactory.createFromName("EPSG:4326")

    val transform = CoordinateTransformFactory().createTransform(osgb36, wgs84)

    val srcPt = ProjCoordinate(easting, northing)
    val dstPt = ProjCoordinate()

    transform.transform(srcPt, dstPt)

    return LatLng(dstPt.y, dstPt.x)
}


@Composable
fun MinesMarkers(
    closedMine: Boolean,
    closingMine: Boolean,
    onSiteSelected: (Mine) -> Unit
) {
    val context = LocalContext.current

    var mines by remember { mutableStateOf<List<Mine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(closedMine, closingMine) {
        isLoading = true
        errorMessage = null
        try {
            mines = RetrofitInstance.mineApi.getAllMines()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to load mines: ${e.message}"
            mines = emptyList()
        }
        isLoading = false
    }

    val filteredMines = mines.filter {
        (closedMine && it.status == "C") || (closingMine && it.status == "I")
    }

    filteredMines.forEach { mine ->
        val latLng = remember(mine.easting, mine.northing) {
            convertOSGB36ToWGS84(mine.easting, mine.northing)
        }

        val pinType = when (mine.status) {
            "C" -> PinType.CLOSED_MINE
            "I" -> PinType.CLOSING_MINE
            else -> PinType.CLOSED_MINE
        }

        val iconBitmap = remember(mine.trend, pinType) {
            createPinBitmap(context, pinType, mine.trend)
        }

        val iconDescriptor = remember(iconBitmap) {
            BitmapDescriptorFactory.fromBitmap(iconBitmap)
        }

        Marker(
            state = MarkerState(position = latLng),
            title = mine.name,
            snippet = when (mine.status) {
                "C" -> "Closed Mine"
                "I" -> "Closing Mine"
                else -> "Mine"
            },
            icon = iconDescriptor,
            onClick = {
                onSiteSelected(mine)
                true
            }
        )
    }
}

fun calculateTrend(data: List<EnergyDemand>): Trend? {
    if (data.size < 2) return null
    val first = data.first().value
    val last = data.last().value
    return when {
        last > first -> Trend.INCREASING
        last < first -> Trend.DECREASING
        else -> Trend.STABLE
    }
}


suspend fun getAllMines(): List<Mine> {
    return try {
        RetrofitInstance.mineApi.getAllMines()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

suspend fun getAllSiteLabelsReferencesAndNames(): List<Triple<String, String, String>> {
    val mines = getAllMines()
    return mines.mapIndexed { index, mine ->
        Triple("Site ${index + 1}", mine.reference, mine.name)
    }
}

suspend fun getInfoByReference(reference: String): Mine? {
    return try {
        RetrofitInstance.mineApi.getMineByReference(reference)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
