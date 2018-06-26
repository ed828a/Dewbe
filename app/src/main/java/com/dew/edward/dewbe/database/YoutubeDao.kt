package com.dew.edward.dewbe.database

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.dew.edward.dewbe.model.VideoModel


/**
 * Created by Edward on 6/26/2018.
 */
@Dao
interface YoutubeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(videos: List<VideoModel>)

    @Query("SELECT * FROM youtube WHERE title LIKE :query ORDER BY indexResponse ASC")
    fun getVideosByQuery(query: String): DataSource.Factory<Int, VideoModel>

    @Query("DELETE FROM youtube WHERE title LIKE :query")
    fun deleteVideosByQuery(query: String)

    @Query("SELECT MAX(indexResponse) + 1 FROM youtube WHERE title LIKE :query")
    fun getNextIndexInVideo(query: String): Int

    @Query("SELECT * FROM youtube ORDER BY indexResponse ASC")
    fun dumpAll(): List<VideoModel>
}