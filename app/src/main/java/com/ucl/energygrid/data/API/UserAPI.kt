package com.ucl.energygrid.data.API

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class RegisterResponse(
    val id: Int,
    val username: String,
    val email: String,
    val hashed_password: String
)
