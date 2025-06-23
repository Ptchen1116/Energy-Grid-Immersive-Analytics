package com.ucl.energygrid.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.BufferedReader
import java.io.InputStreamReader

suspend fun loadEnergyConsumption(context: Context, year: Int): Map<String, Double> = withContext(Dispatchers.IO) {
    val regionToConsumption = mutableMapOf<String, Double>()

    val longToShort = mapOf(
        "E12000001" to "UKC",
        "E12000002" to "UKD",
        "E12000003" to "UKE",
        "E12000004" to "UKF",
        "E12000005" to "UKG",
        "E12000006" to "UKH",
        "E12000007" to "UKI",
        "E12000008" to "UKJ",
        "E12000009" to "UKK",
        "W92000004" to "UKL",
        "S92000003" to "UKM"
    )

    try {
        val assetPath = "Subnational_electricity_consumption_statistics/Subnational_electricity_consumption_statistics_${year}.csv"
        val inputStream = context.assets.open(assetPath)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())

        val headerMap = csvParser.headerMap.keys.associateBy { it.trim().lowercase() }

        val codeKey = headerMap.entries.find { it.key.contains("code") }?.value
        val consumptionKey = headerMap.entries.find {
            it.key.contains("total consumption") && it.key.contains("all meters")
        }?.value

        if (codeKey == null || consumptionKey == null) {
            Log.e("loadEnergy", "Required CSV headers not found. Available headers: ${headerMap.values}")
            return@withContext emptyMap()
        }

        for ((index, record) in csvParser.withIndex()) {
            try {
                val rawCode = record.get(codeKey)?.trim() ?: continue
                val code = if (longToShort.containsKey(rawCode)) longToShort[rawCode]!! else rawCode

                val rawConsumption = record.get(consumptionKey)?.replace(",", "") ?: continue
                val consumption = rawConsumption.toDoubleOrNull() ?: 0.0

                regionToConsumption[code] = consumption
            } catch (e: Exception) {
                Log.w("loadEnergy", "Skipping row $index due to parse error: ${e.message}")
            }
        }

    } catch (e: Exception) {
        Log.e("loadEnergy", "Failed to load energy consumption data for $year: ${e.message}", e)
    }

    regionToConsumption
}