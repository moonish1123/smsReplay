package pe.brice.smsreplay.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from Version 1 to Version 2
 * Added sent_history table for storing successfully sent emails
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create sent_history table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sent_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sender TEXT NOT NULL,
                body TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                recipientEmail TEXT NOT NULL,
                senderEmail TEXT NOT NULL,
                sentAt INTEGER NOT NULL,
                retryCount INTEGER NOT NULL DEFAULT 0
            )
            """
        )

        // Create indexes for performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sent_history_timestamp ON sent_history(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sent_history_sender ON sent_history(sender)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sent_history_body ON sent_history(body)")
    }
}
