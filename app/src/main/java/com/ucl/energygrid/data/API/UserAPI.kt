package com.ucl.energygrid.data.API

import com.ucl.energygrid.data.model.LoginRequest
import com.ucl.energygrid.data.model.LoginResponse
import com.ucl.energygrid.data.model.RegisterRequest
import com.ucl.energygrid.data.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}

