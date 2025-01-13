package com.example.arcanedex_app.data.api

import com.example.arcanedex_app.data.models.CreatureResponse
import com.example.arcanedex_app.data.models.LoginRequest
import com.example.arcanedex_app.data.models.LoginResponse
import com.example.arcanedex_app.data.models.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("/users/register")
    suspend fun registerUser(@Body request: RegisterRequest): Unit

    @POST("/users/login")
    suspend fun loginUser(@Body request: LoginRequest): LoginResponse

    @GET("creatures")
    suspend fun getAllCreatures(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): CreatureResponse
}