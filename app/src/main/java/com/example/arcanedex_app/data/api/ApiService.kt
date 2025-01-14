package com.example.arcanedex_app.data.api

import com.example.arcanedex_app.data.models.CreatureResponse
import com.example.arcanedex_app.data.models.FavoriteRequest
import com.example.arcanedex_app.data.models.LoginRequest
import com.example.arcanedex_app.data.models.LoginResponse
import com.example.arcanedex_app.data.models.RegisterRequest
import com.example.arcanedex_app.data.models.UserProfile
import com.example.arcanedex_app.data.models.UserProfileRequest
import com.example.arcanedex_app.data.models.UserProfileResponse
import com.example.arcanedex_app.data.models.creature.CreatureRequestAdmin
import com.example.arcanedex_app.data.models.creature.CreatureResponseAdmin
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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
        @Query("limit") limit: Int,
        @Query("name") name: String,
        @Query("OnlyFavoriteArcanes") onlyFavoriteArcanes: Boolean,
        @Query("ToSaveOffline") toSaveOffline: Boolean,
    ): CreatureResponse

    @GET("admin/creatures")
    suspend fun getAdminAllCreatures(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("name") name: String
    ): Response<CreatureResponse>

    @GET("users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfile>

    @PUT("users/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body userProfile: UserProfileRequest
    ): Response<UserProfileResponse>

    @POST("creatures/favourites")
    suspend fun addCreatureToFavorites(
        @Header("Authorization") token: String,
        @Body favoriteRequest: FavoriteRequest
    ): Response<Unit>

    @DELETE("creatures/favourites/{id}")
    suspend fun removeCreatureFromFavorites(
        @Header("Authorization") token: String,
        @Path("id") creatureId: Int
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @PUT("creatures/favourites/{id}/background")
    suspend fun changeFavouriteCreatureBackground(
        @Header("Authorization") token: String,
        @Path("id") creatureId: Int,
        @Body request: BackgroundImageRequest
    ): Response<Unit>

    @PUT("/creatures/favourites/{id}/background/default")
    suspend fun resetFavouriteCreatureBackground(
        @Path("id") creatureId: Int,
        @Header("Authorization") token: String
    ): Response<Void>

    @GET("creatures/{id}")
    suspend fun getCreatureDetails(
        @Path("id") creatureId: Int,
        @Header("Authorization") token: String
    ): Response<CreatureDetailsResponse>

    @POST("/admin/creatures")
    suspend fun addCreature(
        @Body creature: CreatureRequestAdmin,
        @Header("Authorization") token: String
    ): Response<CreatureResponseAdmin>

    @PUT("/admin/creatures/{id}")
    suspend fun editCreature(
        @Path("id") id: Int,
        @Body creature: CreatureRequestAdmin,
        @Header("Authorization") token: String
    ): Response<Void>
}

data class BackgroundImageRequest(val BackgroundImg: String)

data class CreatureDetailsResponse(
    val BackgroundImg: String?
)
