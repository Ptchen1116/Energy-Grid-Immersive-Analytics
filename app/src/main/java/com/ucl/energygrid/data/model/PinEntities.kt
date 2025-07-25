package com.ucl.energygrid.data.model

data class PinRequest(
    val mine_id: Int,
    val note: String
)

data class PinResponse(
    val mine_id: Int,
    val note: String,
    val id: Int
)