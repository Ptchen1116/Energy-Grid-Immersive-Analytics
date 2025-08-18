package com.ucl.energygrid.data.repository

import com.auth0.android.jwt.JWT
import com.ucl.energygrid.data.model.LoginRequest
import com.ucl.energygrid.data.model.RegisterRequest
import com.ucl.energygrid.data.remote.apis.UserApi


class UserRepository(private val userApi: UserApi) {

    suspend fun login(email: String, password: String): Result<Pair<String, String>> {
        return try {
            val response = userApi.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val loginResponse = response.body()
                val token = loginResponse?.access_token
                if (token != null) {
                    val jwt = JWT(token)
                    val userId = jwt.getClaim("sub").asString() ?: jwt.getClaim("id").asString()
                    if (!userId.isNullOrEmpty()) {
                        Result.success(Pair(userId, token))
                    } else {
                        Result.failure(Exception("Invalid userId in token"))
                    }
                } else {
                    Result.failure(Exception("No token received"))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit> {
        return try {
            val response = userApi.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Register failed: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}