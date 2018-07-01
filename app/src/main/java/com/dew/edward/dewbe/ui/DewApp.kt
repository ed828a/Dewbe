package com.dew.edward.dewbe.ui

import android.support.multidex.MultiDexApplication
import android.support.v4.content.LocalBroadcastManager
import com.google.android.youtube.player.YouTubePlayer


/**
 * Created by Edward on 6/26/2018.
 */
class DewApp: MultiDexApplication() {
    companion object {
        lateinit var localBroadcastManager: LocalBroadcastManager
        var mYoutubePlayer: YouTubePlayer? = null
    }

    override fun onCreate() {
        super.onCreate()
        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)
    }
}