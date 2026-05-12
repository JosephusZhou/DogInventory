package com.doginventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doginventory.data.dao.InventoryDao
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.data.entity.ShoppingItemEntity
import java.io.File

@Database(
    entities = [
        InventoryCategoryEntity::class,
        InventoryItemEntity::class,
        InventoryReminderRuleEntity::class,
        ShoppingItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        const val DATABASE_NAME = "dog_inventory_database"
        const val DATABASE_VERSION = 5

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Note: In a real app, use a worker or a coroutine to seed data
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun databaseFile(context: Context): File = context.getDatabasePath(DATABASE_NAME)

        fun exportConsistentSnapshot(context: Context, targetFile: File) {
            val db = getDatabase(context).openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").close()
            db.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
            targetFile.parentFile?.mkdirs()
            databaseFile(context).copyTo(targetFile, overwrite = true)
        }

        private val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        isDone INTEGER NOT NULL DEFAULT 0,
                        doneAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        isDone INTEGER NOT NULL DEFAULT 0,
                        doneAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS inventory_items_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        categoryId TEXT,
                        quantityCurrent REAL,
                        quantityUnit TEXT NOT NULL,
                        quantityLowThreshold REAL,
                        expireAt INTEGER,
                        note TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES inventory_categories(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO inventory_items_new (
                        id,
                        name,
                        categoryId,
                        quantityCurrent,
                        quantityUnit,
                        quantityLowThreshold,
                        expireAt,
                        note,
                        status,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        id,
                        name,
                        categoryId,
                        quantityCurrent,
                        quantityUnit,
                        quantityLowThreshold,
                        expireAt,
                        note,
                        status,
                        createdAt,
                        updatedAt
                    FROM inventory_items
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE inventory_items")
                db.execSQL("ALTER TABLE inventory_items_new RENAME TO inventory_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_items_categoryId ON inventory_items(categoryId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_inventory_items_status_updatedAt ON inventory_items(status, updatedAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_shopping_items_isDone_updatedAt ON shopping_items(isDone, updatedAt)"
                )
            }
        }
    }
}
