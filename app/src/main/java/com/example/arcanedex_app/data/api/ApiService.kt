package com.example.arcanedex_app.data.api

import com.example.arcanedex_app.data.models.CreatureResponse
import com.example.arcanedex_app.data.models.LoginRequest
import com.example.arcanedex_app.data.models.LoginResponse
import com.example.arcanedex_app.data.models.RegisterRequest
import com.example.arcanedex_app.data.models.UserProfile
import com.example.arcanedex_app.data.models.UserProfileRequest
import com.example.arcanedex_app.data.models.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {
    @POST("/users/register")
    suspend fun registerUser(@Body request: RegisterRequest): Unit

    @POST("/users/login")
    suspend fun loginUser(@Body request: LoginRequest): LoginResponse

    @GET("creatures")
    suspend fun getAllCreatures(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): CreatureResponse

    @GET("users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfile>

    @PUT("users/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body userProfile: UserProfileRequest
    ): Response<UserProfileResponse>

}