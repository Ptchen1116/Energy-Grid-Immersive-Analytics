package com.ucl.energygrid.data.remote.apis

import retrofit2.http.GET
import retrofit2.http.Path
import com.ucl.energygrid.data.model.Mine

interface MineApi {
    @GET("/mines")
    suspend fun getAllMines(): List<Mine>

    @GET("/mines/{reference}")
    suspend fun getMineByReference(@Path("reference") reference: String): Mine
}