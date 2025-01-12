package com.example.arcanedex_app.data.api
import com.example.arcanedex_app.data.models.LoginRequest
import com.example.arcanedex_app.data.models.LoginResponse
import com.example.arcanedex_app.data.models.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/users/register")
    suspend fun registerUser(@Body request: RegisterRequest): Unit

    @POST("/users/login")
    suspend fun loginUser(@Body request: LoginRequest): LoginResponse
}