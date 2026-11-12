package com.frame.app.platform.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.frame.app.platform.db.dao.ReferenceDao
import com.frame.app.platform.db.entity.ReferenceEntity

/**
 * The app's Room database. Phase 1 ships the schema (reference profiles) but the
 * app writes nothing yet; it exists so Phases 5-6 drop in without a migration
 * from "no database" to "database". When the schema evolves, bump [version] and
 * add a migration.
 */
@Database(
    entities = [ReferenceEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FrameDatabase : RoomDatabase() {

    abstract fun referenceDao(): ReferenceDao

    companion object {
        @Volatile
        private var instance: FrameDatabase? = null

        fun get(context: Context): FrameDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FrameDatabase::class.java,
                    "frame.db",
                ).build().also { instance = it }
            }
    }
}
