// AppDatabase.kt
package com.example.absensiaparatbtp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AbsensiEntity::class, UserEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun absensiDao(): AbsensiDao
    abstract fun userDao(): UserDao

    companion object {

        // Jika dulu versi 1 hanya punya absensi tanpa kolom keteranganAreaTugas dan tanpa tabel users
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE absensi ADD COLUMN keteranganAreaTugas TEXT")
                createUsersTableIfNeeded(db)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createUsersTableIfNeeded(db)
                ensureAbsensiAreaColumn(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createUsersTableIfNeeded(db)
                ensureAbsensiAreaColumn(db)
            }
        }

        private fun createUsersTableIfNeeded(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `users` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `nama` TEXT NOT NULL,
                    `noHp` TEXT NOT NULL,
                    `jenisKelamin` TEXT NOT NULL,
                    `jabatan` TEXT NOT NULL,
                    `alamat` TEXT NOT NULL,
                    `username` TEXT NOT NULL,
                    `password` TEXT NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun ensureAbsensiAreaColumn(db: SupportSQLiteDatabase) {
            val cursor = db.query("PRAGMA table_info(absensi)")
            var hasColumn = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "keteranganAreaTugas") {
                    hasColumn = true
                    break
                }
            }
            cursor.close()
            if (!hasColumn) {
                db.execSQL("ALTER TABLE absensi ADD COLUMN keteranganAreaTugas TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "absensi_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    // Aman untuk development: kalau masih mismatch, DB lama akan di-destroy dan dibuat baru
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}