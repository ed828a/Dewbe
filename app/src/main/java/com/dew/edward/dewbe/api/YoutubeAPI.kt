package com.dew.edward.dewbe.api


import android.net.Uri
import android.util.Log
import com.dew.edward.dewbe.model.YoutubeResponseData
import com.dew.edward.dewbe.util.API_KEY
import com.dew.edward.dewbe.util.NETWORK_PAGE_SIZE
import com.dew.edward.dewbe.util.YOUTUBE_BASE_URL
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * Created by Edward on 6/26/2018.
 */
interface YoutubeAPI {

    @GET("search")
    fun searchVideo(@Query("q") query: String = "",
                    @Query("pageToken") pageToken: String ="",
                    @Query("part") part: String = "snippet",
                    @Query("maxResults") maxResults: String = "$NETWORK_PAGE_SIZE",
                    @Query("type") type: String = "video",
                    @Query("key") key: String = API_KEY): Call<YoutubeResponseData>

    @GET("search")
    fun getRelatedVideos(@Query("relatedToVideoId") relatedToVideoId: String = "",
                         @Query("pageToken") pageToken: String = "",
                         @Query("part") part: String = "snippet",
                         @Query("maxResults") maxResults: String = "$NETWORK_PAGE_SIZE",
                         @Query("type") type: String = "video",
                         @Query("key") key: String = API_KEY): Call<YoutubeResponseData>

    companion object {

        fun create(): YoutubeAPI = createYoutubeApi(HttpUrl.parse(YOUTUBE_BASE_URL)!!)
        private fun createYoutubeApi(httpUrl: HttpUrl): YoutubeAPI {
            val logger = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                Log.d("YoutubeAPI", it)
            })
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val okHttpClient = OkHttpClient.Builder().addInterceptor(logger).build()

            return Retrofit.Builder()
                    .baseUrl(httpUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(YoutubeAPI::class.java)
        }
    }
}