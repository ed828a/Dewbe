package com.dew.edward.dewbe.ui

import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.dew.edward.dewbe.R
import com.dew.edward.dewbe.adapter.VideoModelAdapter
import com.dew.edward.dewbe.model.NetworkState
import com.dew.edward.dewbe.model.VideoModel
import com.dew.edward.dewbe.util.API_KEY
import com.dew.edward.dewbe.util.GlideApp
import com.dew.edward.dewbe.util.VIDEO_MODEL
import com.dew.edward.dewbe.viewmodel.FraudDbViewModel
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import kotlinx.android.synthetic.main.activity_video_play.*
import kotlinx.android.synthetic.main.content_list.*

class VideoPlayActivity : YouTubeBaseActivity(), YouTubePlayer.OnInitializedListener {

    lateinit var videoModel: VideoModel
    var isRelatedVideo: Boolean = false
    lateinit var listView: RecyclerView
    private lateinit var queryViewModel: FraudDbViewModel

    lateinit var adapter: VideoModelAdapter

    private val videoListObserver =
            object : Observer<PagedList<VideoModel>> {
                override fun onChanged(videoList: PagedList<VideoModel>?) {
                    adapter.submitList(videoList)
                }
            }

    private val networkStateObserver =
            object : Observer<NetworkState?> {
                override fun onChanged(networkState: NetworkState?) {
                    adapter.setNetworkState(networkState)
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_play)

        queryViewModel = FraudDbViewModel(this)

        videoModel = intent.getParcelableExtra(VIDEO_MODEL)
        youtubePlayer.initialize(API_KEY, this)

        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            textVideoPlayTitle?.text = videoModel.title

            initRelatedList()
            initSearch()

            queryViewModel.showRelatedToVideoId(videoModel.videoId)
        }
    }
    private fun initRelatedList() {
        listView = recyclerRelatedListView
        listView.layoutManager = GridLayoutManager(this, 2)
        val glide = GlideApp.with(this)
        adapter = VideoModelAdapter(
                { queryViewModel.retry() },
                {
                    DewApp.mYoutubePlayer?.release()
                    youtubePlayer.initialize(API_KEY, this)
                    textVideoPlayTitle?.text = it.title
                    isRelatedVideo = true
                    intent.putExtra(VIDEO_MODEL, it)

                    if (queryViewModel.showRelatedToVideoId(it.videoId)) {
                        listView.scrollToPosition(0)
                        (listView.adapter as? VideoModelAdapter)?.submitList(null)
                    }
                })
        listView.adapter = adapter

        queryViewModel.videoList.observeForever(videoListObserver)
        queryViewModel.networkState.observeForever(networkStateObserver)
    }

    private fun initSearch() {
        buttonSearch.setOnSearchClickListener {
            buttonDownload.visibility = View.GONE
            textVideoPlayTitle.visibility = View.GONE

            buttonSearch.onActionViewExpanded()
        }

        buttonSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.trim()?.let {
                    if (it.isNotEmpty()) {
                        if (queryViewModel.showSearchQuery(it)) {
                            listView.scrollToPosition(0)
                            (listView.adapter as? VideoModelAdapter)?.submitList(null)
                        }
                    }
                }

                buttonSearch.onActionViewCollapsed()
//                hideKeyboard()
                buttonDownload.visibility = View.VISIBLE
                textVideoPlayTitle.visibility = View.VISIBLE

                Log.d("onQueryTextSubmit", "queryString: $query")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        val closeButton = buttonSearch.findViewById<ImageView>(R.id.search_close_btn)
        closeButton.setOnClickListener {
            buttonSearch.onActionViewCollapsed()
            buttonDownload.visibility = View.VISIBLE
            textVideoPlayTitle.visibility = View.VISIBLE
        }
    }

    override fun onInitializationSuccess(provider: YouTubePlayer.Provider?,
                                         player: YouTubePlayer?,
                                         wasRestored: Boolean) {

        player?.setPlayerStateChangeListener(playerStateChangeListener)
        player?.setPlaybackEventListener(playbackEventListener)
        videoModel = intent.getParcelableExtra(VIDEO_MODEL)

        if (!wasRestored || isRelatedVideo) {
            player?.cueVideo(videoModel.videoId)
        }

        if (isRelatedVideo){
            isRelatedVideo = false
        }

        if (player != null) {
            Log.e("Rotate", "App.mYoutubePlayer = player:  ${player.toString()}")
            DewApp.mYoutubePlayer = player
        }
    }

    override fun onInitializationFailure(p0: YouTubePlayer.Provider?, p1: YouTubeInitializationResult?) {

    }


    private val playerStateChangeListener: YouTubePlayer.PlayerStateChangeListener =
            object : YouTubePlayer.PlayerStateChangeListener {
                override fun onAdStarted() {
                }

                override fun onLoading() {
                }

                override fun onVideoStarted() {
                }

                override fun onLoaded(videoId: String?) {
                    Log.e("Rotate", "onLoaded: $videoId")
                    DewApp.mYoutubePlayer?.play()
                }

                override fun onVideoEnded() {
                }

                override fun onError(p0: YouTubePlayer.ErrorReason?) {
                }

            }

    private val playbackEventListener: YouTubePlayer.PlaybackEventListener =
            object : YouTubePlayer.PlaybackEventListener {
                override fun onSeekTo(p0: Int) {
                }

                override fun onBuffering(p0: Boolean) {
                }

                override fun onPlaying() {
                }

                override fun onStopped() {
                }

                override fun onPaused() {
                }
            }

    override fun onDestroy() {
        if (DewApp.mYoutubePlayer != null){
            DewApp.mYoutubePlayer?.release()
        }

        queryViewModel.videoList.removeObserver(videoListObserver)
        queryViewModel.networkState.removeObserver(networkStateObserver)

        super.onDestroy()
    }
}
