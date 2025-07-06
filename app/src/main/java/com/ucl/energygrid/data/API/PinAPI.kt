package com.ucl.energygrid.data.API

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

data class PinRequest(
    val mine_id: Int,
    val note: String
)

data class PinResponse(
    val mine_id: Int,
    val note: String,
    val id: Int
)

interface PinApi {
    @POST("/api/users/{user_id}/pins")
    suspend fun create_or_update_pin(
        @Path("user_id") userId: Int,
        @Body pinRequest: PinRequest
    ): Response<PinResponse>

    @GET("/api/users/{user_id}/pins/mine/{mine_id}")
    suspend fun getPin(
        @Path("user_id") userId: Int,
        @Path("mine_id") mineId: Int
    ): Response<PinResponse>
}