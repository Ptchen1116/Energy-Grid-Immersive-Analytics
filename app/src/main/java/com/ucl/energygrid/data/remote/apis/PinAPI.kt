package com.ucl.energygrid.data.remote.apis

import com.ucl.energygrid.data.model.PinRequest
import com.ucl.energygrid.data.model.PinResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


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

    @GET("/api/users/{user_id}/pins")
    suspend fun getAllPins(
        @Path("user_id") userId: Int
    ): Response<List<PinResponse>>

    @DELETE("/api/users/{user_id}/pins/mine/{mine_id}")
    suspend fun deletePin(
        @Path("user_id") userId: Int,
        @Path("mine_id") mineId: Int
    ): Response<Unit>
}