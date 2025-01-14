package pt.ipt.arcanedex_app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pt.ipt.arcanedex_app.data.models.creature.ArcaneEntity
import pt.ipt.arcanedex_app.data.utils.ArcaneDao

/**
 * Classe responsável por criar e gerir a base de dados local da aplicação usando Room.
 * A base de dados armazena entidades do tipo [ArcaneEntity].
 */
@Database(entities = [ArcaneEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Fornece acesso ao DAO para interagir com as tabelas da base de dados.
     */
    abstract fun arcaneDao(): ArcaneDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtém a instância singleton da base de dados.
         *
         * @param context Contexto da aplicação necessário para inicializar a base de dados.
         * @return Instância da base de dados.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Se a instância já existe, retorna-a. Caso contrário, inicializa-a sincronizadamente.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arcane_database" // Nome do ficheiro da base de dados
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
