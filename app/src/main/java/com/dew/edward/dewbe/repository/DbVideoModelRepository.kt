package com.dew.edward.dewbe.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.content.Context
import android.os.Environment
import android.support.annotation.MainThread
import android.util.Log
import com.dew.edward.dewbe.api.YoutubeAPI
import com.dew.edward.dewbe.database.YoutubeDb
import com.dew.edward.dewbe.model.*
import com.dew.edward.dewbe.util.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by Edward on 6/26/2018.
 */
class DbVideoModelRepository(context: Context) {

    val db: YoutubeDb by lazy {
        YoutubeDb.create(context, false)  // useInMemory meaningless here as we don't want to load all items in mem at once.
    }

    val webService = YoutubeAPI.create()
    val ioExecutor: Executor = Executors.newFixedThreadPool(5)
    private val onceExecutor: Executor = Executors.newSingleThreadExecutor()

    private val pageStatus = PageInfo()
    private val helper = PagingRequestHelper(ioExecutor)
    private val networkState = helper.createStatusLiveData()


    companion object {
        const val TAG = "DbVideoModelRepository"
        /**
         * Inserts the response into the database while also assigning position indices to items.
         */
        fun insertResultIntoDb(db: YoutubeDb, queryData: QueryData, items: List<VideoModel>) {

            db.runInTransaction {
                val start = db.youtubeDao().getNextIndexInVideo()
                val indexedItems = items.mapIndexed { index, video ->
                    video.indexResponse = start + index
                    if (queryData.type == Type.RELATED_VIDEO_ID) {
                        video.relatedToVideoId = queryData.query
                    }
                    video
                }

                db.youtubeDao().insert(indexedItems)
            }
        }

        fun createWebserviceCallback(
                db: YoutubeDb,
                queryData: QueryData,
                helperRequestCallback: PagingRequestHelper.Request.Callback,
                ioExecutor: Executor,
                pageInfo: PageInfo) = object : Callback<YoutubeResponseData> {

            override fun onFailure(call: Call<YoutubeResponseData>?, t: Throwable?) {
                helperRequestCallback.recordFailure(t!!)
                Log.d(TAG, "networkState ERROR: ${t.message}")
            }

            override fun onResponse(call: Call<YoutubeResponseData>?, response: Response<YoutubeResponseData>?) {
                if (response != null && response.isSuccessful) {
                    ioExecutor.execute {
                        val data = response.body()
                        Log.d(TAG, "data: ${data.toString()}")
                        val mappedItems = data?.items?.map {
                            val video = VideoModel(it.snippet.title,
                                    it.snippet.publishedAt.extractDate(),
                                    it.snippet.thumbnails.high.url,
                                    it.id.videoId)

                            video
                        }

                        // update pageTokens
                        pageInfo.prevPage = data?.prevPageToken ?: ""
                        pageInfo.nextPage = data?.nextPageToken ?: ""
                        pageInfo.totalResults = data?.pageInfo?.totalResults ?: ""

                        db.runInTransaction {
                            if (queryData.type == Type.RELATED_VIDEO_ID) {
                                db.youtubeDao().deleteVideosByRelatedToVideoId(queryData.query)
                            }
                            insertResultIntoDb(db, queryData, mappedItems!!)
                            val dataDump = db.youtubeDao().dumpAll()
                            Log.d(TAG, " onResponse, after insertResultIntoDb, dumpAll: $dataDump")
                        }
                        // since we are in bg thread now, post the result.
                        // help Request Callback will update the NetWorkState
                        helperRequestCallback.recordSuccess()
                    }
                } else {
                    Log.d(TAG, "onResponse Error: response = null or response isn't successful")
                }
            }
        }
    }

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(queryData: QueryData): LiveData<NetworkState> {

        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            val call = if (queryData.type == Type.QUERY_STRING) {
                webService.searchVideo(queryData.query, pageStatus.nextPage)
            } else {
                webService.getRelatedVideos(queryData.query)
            }

            call.enqueue(createWebserviceCallback(db, queryData, it, ioExecutor, pageStatus))
        }
        return networkState
    }

    /**
     * Returns a Listing for the given query string.
     */
    @MainThread
    fun searchVideosOnYoutube(queryData: QueryData): Listing<VideoModel> {
        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        Log.d(TAG, "postsOfSearchYoutube called, query =${queryData.query}")
        // reset db.table on every new query
        onceExecutor.execute {
            db.youtubeDao().deleteVideosByQuery()
        }

        val boundaryCallback = VideosBoundaryCallback(queryData)
        val dataSourceFactory = if (queryData.type == Type.QUERY_STRING) {
            db.youtubeDao().getVideosByQuery()
        } else {
            db.youtubeDao().getVideosByRelatedToVideoId(queryData.query)
        }

        val pagedList = LivePagedListBuilder(dataSourceFactory, DATABASE_PAGE_SIZE)
                .setBoundaryCallback(boundaryCallback)

        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        // so this part code should locate in ViewModel, not here, and unit should be Query String
        val refreshTriger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTriger) { refresh(queryData) }

        return Listing(
                pagedList.build(),
                networkState,
                retry = { helper.retryAllFailed() },
                refresh = { refreshTriger.value = null },
                refreshState = refreshState
        )
    }

    fun dumpDb(queryData: QueryData) {
        val stub = db.youtubeDao().dumpAll()
        Log.d(TAG, "DB stub: $stub")
    }

    fun resetPageStatus() {
        pageStatus.prevPage = ""
        pageStatus.nextPage = ""
        pageStatus.totalResults = ""
    }

    inner class VideosBoundaryCallback(private val queryData: QueryData) : PagedList.BoundaryCallback<VideoModel>() {

        private var lastQuery: String = ""

        /**
         * Database returned 0 items. We should query the backend for more items.
         * initialize a new query
         */
        @MainThread
        override fun onZeroItemsLoaded() {
            lastQuery = queryData.query
            // temporary for testing
            ioExecutor.execute { dumpDb(queryData) }

            helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL)
            {
                val call = if (queryData.type == Type.QUERY_STRING) {
                    webService.searchVideo(queryData.query)
                } else {
                    webService.getRelatedVideos(queryData.query)
                }
                call.enqueue(createWebserviceCallback(db, queryData, it, ioExecutor, pageStatus))
            }
        }

        /**
         * User reached to the end of the list.
         */
        override fun onItemAtEndLoaded(itemAtEnd: VideoModel) {
            if (pageStatus.nextPage.isEmpty()) {
                Log.d(TAG, "nextPage = ${pageStatus.nextPage}, return")
                return
            }

            helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
                val call = if (queryData.type == Type.QUERY_STRING) {
                    webService.searchVideo(queryData.query, pageStatus.nextPage)
                } else {
                    webService.getRelatedVideos(queryData.query, pageStatus.nextPage)
                }
                call.enqueue(createWebserviceCallback(db, queryData, it, ioExecutor, pageStatus))
            }
        }

        override fun onItemAtFrontLoaded(itemAtFront: VideoModel) {
            Log.d(TAG, "onItemAtFrontLoaded called:")
            // ignored, since we only ever append to what's in the DB
        }
    }

    fun downloadVideo(urlString: String, fileName: String){
    }

    private fun writeResponseBodyToDisk(fileName: String, responseBody: ResponseBody): Boolean {
        val fileFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        Log.d(TAG, "filename = $fileFolder/$fileName")
        val file = File(fileFolder, "$fileName.mp4")
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val fileReader = ByteArray(4096)
            val fileSize = responseBody.contentLength()
            var fileSizeDownloaded: Long = 0

            inputStream = responseBody.byteStream()
            outputStream = FileOutputStream(file)

            while (true){
                val read = inputStream.read(fileReader)
                if (read == -1){
                    break
                }
                outputStream.write(fileReader, 0, read)
                fileSizeDownloaded += read.toLong()

                Log.d(TAG, "Downloading progress: $fileSizeDownloaded of $fileSize")
            }
            outputStream.flush()

            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    val foryou = fun (urlString: String, fileName: String){
        val call = webService.downloadVideoByUrlStream(urlString)
        call.enqueue(object : Callback<ResponseBody>{
            override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                t?.printStackTrace()
                Log.d(TAG, "downloading failed: ${t?.message}")
            }

            override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                if (response != null && response.isSuccessful){
                    onceExecutor.execute {
                        val isSuccess = writeResponseBodyToDisk(fileName, response.body()!!)
                    }
                } else {
                    Log.d(TAG, "Response Error: ${response?.message()}")
                }
            }
        })
    }
}