package com.ucl.energygrid.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.platform.LocalContext
import org.locationtech.proj4j.*
import org.json.JSONArray
import com.ucl.energygrid.ui.component.PinType
import com.ucl.energygrid.ui.screen.Mine
import com.ucl.energygrid.ui.screen.EnergyDemand
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun convertOSGB36ToWGS84(easting: Double, northing: Double): LatLng {
    val csFactory = CRSFactory()
    val osgb36 = csFactory.createFromName("EPSG:27700")  // British National Grid
    val wgs84 = csFactory.createFromName("EPSG:4326")    // WGS84 lat/lng

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
    markerIcons: Map<PinType, BitmapDescriptor>,
    onSiteSelected: (Mine) -> Unit
) {
    val context = LocalContext.current

    val minesJson = remember {
        context.assets.open("fake_mine_location_data.json").bufferedReader().use { it.readText() }
    }

    val mines = remember(minesJson) { parseMinesFromJson(minesJson) }

    val filteredMines = mines.filter {
        (closedMine && it.status == "C") || (closingMine && it.status == "I")
    }

    filteredMines.forEach { mine ->
        val latLng = remember(mine.easting, mine.northing) {
            convertOSGB36ToWGS84(mine.easting, mine.northing)
        }

        Marker(
            state = MarkerState(position = latLng),
            title = mine.name,
            snippet = if (mine.status == "C") "Closed Mine" else "Closing Mine",
            icon = markerIcons[PinType.MINE] ?: BitmapDescriptorFactory.defaultMarker(),
            onClick = {
                onSiteSelected(mine)
                true
            }
        )
    }
}

fun parseMinesFromJson(jsonString: String): List<Mine> {
    val list = mutableListOf<Mine>()
    val jsonArray = JSONArray(jsonString)

    for (i in 0 until jsonArray.length()) {
        if (jsonArray.isNull(i)) {
            continue
        }

        val obj = jsonArray.getJSONObject(i)

        val reference = obj.optString("Reference")
        val name = obj.optString("Name")
        val status = obj.optString("Status")
        val easting = obj.optDouble("Easting")
        val northing = obj.optDouble("Northing")
        val localAuthority = obj.optString("LocalAuthority")
        val note = if (obj.isNull("Note")) null else obj.optString("Note")
        val floodRiskLevel = if (obj.isNull("FloodRiskLevel")) null else obj.optString("FloodRiskLevel")
        val floodHistory = if (obj.isNull("FloodHistory")) null else obj.optString("FloodHistory")

        val energyDemandList = mutableListOf<EnergyDemand>()
        if (!obj.isNull("EnergyDemandHistory")) {
            val energyArray = obj.getJSONArray("EnergyDemandHistory")
            for (j in 0 until energyArray.length()) {
                if (energyArray.isNull(j)) continue
                val energyObj = energyArray.getJSONObject(j)
                val year = energyObj.optInt("year")
                val value = energyObj.optDouble("value")
                energyDemandList.add(EnergyDemand(year, value))
            }
        }
        val forecastEnergyDemand = null

        list.add(
            Mine(
                reference, name, status,
                easting, northing,
                localAuthority, note,
                floodRiskLevel, floodHistory,
                energyDemandList.takeIf { it.isNotEmpty() },
                forecastEnergyDemand
            )
        )
    }

    return list
}

