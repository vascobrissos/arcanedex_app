package pt.ipt.arcanedex_app.data.utils

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pt.ipt.arcanedex_app.data.models.Creature.ArcaneEntity

/**
 * Interface DAO (Data Access Object) para interagir com a tabela `arcanes` na base de dados.
 * Define as operações de manipulação de dados, como inserir, consultar e apagar registos.
 */
@Dao
interface ArcaneDao {

    /**
     * Insere uma lista de entidades [ArcaneEntity] na base de dados.
     * Se houver conflito (por exemplo, IDs duplicados), os registos existentes serão substituídos.
     *
     * @param arcanes Lista de entidades a serem inseridas.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(arcanes: List<ArcaneEntity>)

    /**
     * Recupera todos os registos da tabela `arcanes`.
     *
     * @return Lista de todas as entidades armazenadas na tabela.
     */
    @Query("SELECT * FROM arcanes")
    fun getAllArcanes(): List<ArcaneEntity>

    /**
     * Remove todos os registos da tabela `arcanes`.
     * Usado para limpar a cache da base de dados.
     */
    @Query("DELETE FROM arcanes")
    fun clearCache()
}
