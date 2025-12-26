package pe.brice.smsreplay.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import pe.brice.smsreplay.data.local.dao.PendingSmsDao
import pe.brice.smsreplay.data.dao.SentHistoryDao
import pe.brice.smsreplay.data.local.migrations.MIGRATION_1_2

/**
 * Room Database for SMS Replay App
 * Version 2 - Added sent history table
 */
@Database(
    entities = [PendingSmsEntity::class, SentHistoryEntity::class],
    version = 2,
    exportSchema = true
)
abstract class SmsDatabase : RoomDatabase() {

    abstract fun pendingSmsDao(): PendingSmsDao
    abstract fun sentHistoryDao(): SentHistoryDao

    companion object {
        private const val DATABASE_NAME = "sms_replay_database"

        @Volatile
        private var INSTANCE: SmsDatabase? = null

        /**
         * Get database instance (singleton)
         */
        fun getDatabase(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback(context))
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Database callback for initialization
         */
        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate database if needed
                // Currently no seed data required
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Perform any migrations or validation on database open
                // Currently no additional logic needed
            }
        }

        /**
         * Destroy database instance (for testing)
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
