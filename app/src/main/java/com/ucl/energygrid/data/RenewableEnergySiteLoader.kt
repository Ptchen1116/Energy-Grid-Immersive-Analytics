package com.ucl.energygrid.data

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import android.util.Log
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate

fun readAndExtractSitesByType(
    context: Context,
    filename: String = "renewable_energy_planning_database.csv",
    category: String
): List<Triple<String, Double, Double>> {

    val inputStream = context.assets.open(filename)
    val rows: List<Map<String, String>> = csvReader {
        charset = "UTF-8"
    }.readAllWithHeader(inputStream)

    val categoryToTypes = mapOf(
        "solar" to setOf("Solar Photovoltaics"),
        "wind" to setOf("Wind Offshore", "Wind Onshore"),
        "hydroelectric" to setOf("Large Hydro", "Small Hydro", "Pumped Storage Hydroelectricity")
    )
    val targetTypes = categoryToTypes[category.lowercase()] ?: return emptyList()

    val crsFactory = CRSFactory()
    val sourceCRS = crsFactory.createFromName("EPSG:27700") // British National Grid
    val targetCRS = crsFactory.createFromName("EPSG:4326")  // WGS84
    val transform = CoordinateTransformFactory().createTransform(sourceCRS, targetCRS)

    val results = mutableListOf<Triple<String, Double, Double>>() // (Site Name, Lat, Lon)

    for (row in rows) {
        val type = row["Technology Type"]?.trim()
        val statusShort = row["Development Status (short)"]?.trim()
        val siteName = row["Site Name"] ?: continue
        val x = row["X-coordinate"]?.toDoubleOrNull()
        val y = row["Y-coordinate"]?.toDoubleOrNull()

        if (type in targetTypes && statusShort == "Operational" && x != null && y != null) {
            val srcCoord = ProjCoordinate(x, y)
            val dstCoord = ProjCoordinate()
            transform.transform(srcCoord, dstCoord)
            results.add(Triple(siteName, dstCoord.y, dstCoord.x)) // (Site Name, Lat, Lon)
        }
    }

    return results
}