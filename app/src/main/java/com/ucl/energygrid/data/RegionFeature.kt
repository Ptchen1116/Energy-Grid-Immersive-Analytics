package com.ucl.energygrid.data

data class RegionFeature(
    val name: String,
    val nutsCode: String,
    val polygons: List<List<List<Double>>>
)