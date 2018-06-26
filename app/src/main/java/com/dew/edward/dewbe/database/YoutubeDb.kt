package com.dew.edward.dewbe.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.dew.edward.dewbe.model.VideoModel


/**
 * Created by Edward on 6/26/2018.
 */
@Database(
        entities = [(VideoModel::class)],
        version = 2,
        exportSchema = false
)
abstract class YoutubeDb: RoomDatabase() {
    abstract fun youtubeDao(): YoutubeDao

    companion object {
        fun create(context: Context, useInMemory: Boolean): YoutubeDb {
            val dbBuilder = if (useInMemory) {
                Room.inMemoryDatabaseBuilder(context, YoutubeDb::class.java)
            } else {
                Room.databaseBuilder(context, YoutubeDb::class.java, "videos.db")
            }

            return dbBuilder
                    .fallbackToDestructiveMigration()  // because db is cache, deleting old data is fine
                    .build()
        }
    }
}