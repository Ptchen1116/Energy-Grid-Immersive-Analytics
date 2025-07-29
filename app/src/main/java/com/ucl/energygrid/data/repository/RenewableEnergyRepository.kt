package com.ucl.energygrid.data.repository

import android.util.Log
import com.ucl.energygrid.data.remote.apis.RetrofitInstance


suspend fun readAndExtractSitesByType(category: String): List<Triple<String, Double, Double>> {
    return try {
        val sites = RetrofitInstance.siteApi.getSitesByCategory(category)
        sites.map { Triple(it.name, it.lat, it.lon) }
    } catch (e: Exception) {
        Log.e("FetchSites", "Error fetching $category sites: ${e.message}", e)
        emptyList()
    }
}