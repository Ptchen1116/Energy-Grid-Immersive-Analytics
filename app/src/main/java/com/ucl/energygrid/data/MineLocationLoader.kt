package com.ucl.energygrid.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.platform.LocalContext
import org.locationtech.proj4j.*
import org.json.JSONArray
import com.ucl.energygrid.ui.component.PinType



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

data class Mine(
    val reference: String,
    val name: String,
    val status: String,
    val easting: Double,
    val northing: Double
)

@Composable
fun MinesMarkers(
    closedMine: Boolean,
    closingMine: Boolean,
    markerIcons: Map<PinType, BitmapDescriptor>
) {
    val context = LocalContext.current

    val minesJson = remember {
        context.assets.open("fake_mine_location_data.json").bufferedReader().use { it.readText() }
    }

    val mines = remember(minesJson) {
        parseMinesFromJson(minesJson)
    }

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
            icon = markerIcons[PinType.MINE] ?: BitmapDescriptorFactory.defaultMarker()
        )
    }
}

fun parseMinesFromJson(jsonString: String): List<Mine> {
    val list = mutableListOf<Mine>()
    val jsonArray = JSONArray(jsonString)

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)

        val reference = obj.optString("Reference")
        val name = obj.optString("Name")
        val status = obj.optString("Status")
        val easting = obj.optDouble("Easting")
        val northing = obj.optDouble("Northing")

        list.add(Mine(reference, name, status, easting, northing))
    }

    return list
}