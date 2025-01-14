package pt.ipt.arcanedex_app.data.api

/**
 * Interface que define os endpoints da API e os seus respetivos métodos.
 */
import pt.ipt.arcanedex_app.data.models.creature.CreatureResponse
import pt.ipt.arcanedex_app.data.models.creature.FavoriteRequest
import pt.ipt.arcanedex_app.data.models.user.LoginRequest
import pt.ipt.arcanedex_app.data.models.user.LoginResponse
import pt.ipt.arcanedex_app.data.models.user.RegisterRequest
import pt.ipt.arcanedex_app.data.models.user.UserProfile
import pt.ipt.arcanedex_app.data.models.user.UserProfileRequest
import pt.ipt.arcanedex_app.data.models.user.UserProfileResponse
import pt.ipt.arcanedex_app.data.models.creature.CreatureRequestAdmin
import pt.ipt.arcanedex_app.data.models.creature.CreatureResponseAdmin
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

    /**
     * Registar um novo utilizador.
     * @param request Objeto que contém os dados necessários para o registo.
     */
    @POST("/users/register")
    suspend fun registerUser(@Body request: RegisterRequest): Unit

    /**
     * Autenticar um utilizador existente.
     * @param request Objeto que contém os dados de autenticação.
     * @return Objeto contendo o token de autenticação.
     */
    @POST("/users/login")
    suspend fun loginUser(@Body request: LoginRequest): LoginResponse

    /**
     * Obter todas as criaturas.
     * @param token Token de autenticação do utilizador.
     * @param page Número da página para paginação.
     * @param limit Limite de itens por página.
     * @param name Filtro pelo nome da criatura.
     * @param onlyFavoriteArcanes Flag para obter apenas favoritos.
     * @param toSaveOffline Flag para obter criaturas para guardar offline.
     * @return Resposta contendo a lista de criaturas e metadados.
     */
    @GET("creatures")
    suspend fun getAllCreatures(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("name") name: String,
        @Query("OnlyFavoriteArcanes") onlyFavoriteArcanes: Boolean,
        @Query("ToSaveOffline") toSaveOffline: Boolean,
    ): CreatureResponse

    /**
     * Obter todas as criaturas como administrador.
     * @param token Token de autenticação do administrador.
     * @param page Número da página para paginação.
     * @param limit Limite de itens por página.
     * @param name Filtro pelo nome da criatura.
     */
    @GET("admin/creatures")
    suspend fun getAdminAllCreatures(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("name") name: String
    ): Response<CreatureResponse>

    /**
     * Obter o perfil do utilizador autenticado.
     * @param token Token de autenticação do utilizador.
     * @return Resposta contendo os dados do perfil do utilizador.
     */
    @GET("users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfile>

    /**
     * Atualizar o perfil do utilizador autenticado.
     * @param token Token de autenticação do utilizador.
     * @param userProfile Objeto contendo os dados a serem atualizados.
     */
    @PUT("users/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body userProfile: UserProfileRequest
    ): Response<UserProfileResponse>

    /**
     * Adicionar uma criatura aos favoritos do utilizador.
     * @param token Token de autenticação do utilizador.
     * @param favoriteRequest Objeto contendo o ID da criatura a ser favoritada.
     */
    @POST("creatures/favourites")
    suspend fun addCreatureToFavorites(
        @Header("Authorization") token: String,
        @Body favoriteRequest: FavoriteRequest
    ): Response<Unit>

    /**
     * Remover uma criatura dos favoritos do utilizador.
     * @param token Token de autenticação do utilizador.
     * @param creatureId ID da criatura a ser removida dos favoritos.
     */
    @DELETE("creatures/favourites/{id}")
    suspend fun removeCreatureFromFavorites(
        @Header("Authorization") token: String,
        @Path("id") creatureId: Int
    ): Response<Unit>

    /**
     * Alterar a imagem de fundo de uma criatura favorita.
     * @param token Token de autenticação do utilizador.
     * @param creatureId ID da criatura.
     * @param request Objeto contendo a imagem de fundo codificada em Base64.
     */
    @Headers("Content-Type: application/json")
    @PUT("creatures/favourites/{id}/background")
    suspend fun changeFavouriteCreatureBackground(
        @Header("Authorization") token: String,
        @Path("id") creatureId: Int,
        @Body request: BackgroundImageRequest
    ): Response<Unit>

    /**
     * Redefinir a imagem de fundo de uma criatura favorita para o padrão.
     * @param creatureId ID da criatura.
     * @param token Token de autenticação do utilizador.
     */
    @PUT("/creatures/favourites/{id}/background/default")
    suspend fun resetFavouriteCreatureBackground(
        @Path("id") creatureId: Int,
        @Header("Authorization") token: String
    ): Response<Void>

    /**
     * Obter os detalhes de uma criatura específica.
     * @param creatureId ID da criatura.
     * @param token Token de autenticação do utilizador.
     */
    @GET("creatures/{id}")
    suspend fun getCreatureDetails(
        @Path("id") creatureId: Int,
        @Header("Authorization") token: String
    ): Response<CreatureDetailsResponse>

    /**
     * Adicionar uma nova criatura como administrador.
     * @param creature Dados da nova criatura.
     * @param token Token de autenticação do administrador.
     */
    @POST("/admin/creatures")
    suspend fun addCreature(
        @Body creature: CreatureRequestAdmin,
        @Header("Authorization") token: String
    ): Response<CreatureResponseAdmin>

    /**
     * Editar os detalhes de uma criatura como administrador.
     * @param id ID da criatura.
     * @param creature Dados atualizados da criatura.
     * @param token Token de autenticação do administrador.
     */
    @PUT("/admin/creatures/{id}")
    suspend fun editCreature(
        @Path("id") id: Int,
        @Body creature: CreatureRequestAdmin,
        @Header("Authorization") token: String
    ): Response<Void>

    /**
     * Excluir a conta do utilizador autenticado.
     * @param token Token de autenticação do utilizador.
     */
    @DELETE("users/deleteAccount")
    suspend fun deleteUserAccount(
        @Header("Authorization") token: String
    ): Response<Unit>
}

/**
 * Modelo de dados para a requisição de alteração de imagem de fundo.
 * @param BackgroundImg Imagem de fundo codificada em Base64.
 */
data class BackgroundImageRequest(val BackgroundImg: String)

/**
 * Modelo de dados para a resposta com detalhes de uma criatura.
 * @param BackgroundImg Imagem de fundo codificada em Base64.
 */
data class CreatureDetailsResponse(
    val BackgroundImg: String?
)
