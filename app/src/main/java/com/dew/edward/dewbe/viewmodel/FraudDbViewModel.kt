package com.dew.edward.dewbe.viewmodel

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import com.dew.edward.dewbe.model.QueryData
import com.dew.edward.dewbe.model.Type
import com.dew.edward.dewbe.repository.DbVideoModelRepository


/**
 * Created by Edward on 6/26/2018.
 */
class FraudDbViewModel(context: Context) {

    val repository = DbVideoModelRepository(context)

    private val relatedToVideoId = MutableLiveData<String>()
    private val queryString = MutableLiveData<String>()
    private var queryData = MediatorLiveData<QueryData>()
    init {
        queryData.addSource(relatedToVideoId) { related ->
            queryData.value = QueryData(related ?: "", Type.RELATED_VIDEO_ID)
        }

        queryData.addSource(queryString) {
            queryData.value = QueryData(it ?: "", Type.QUERY_STRING)
        }
    }

    private val searchResult =
            Transformations.map(queryData) { queryData ->
                with(repository) {
                    resetPageStatus()
                    searchVideosOnYoutube(queryData)
                }
            }

    val videoList = Transformations.switchMap(searchResult) { it.pagedList }!!
    val networkState = Transformations.switchMap(searchResult) { it.networkState }!!
    val refreshState = Transformations.switchMap(searchResult) { it.refreshState }!!

    fun refresh() {
        searchResult.value?.refresh?.invoke()
    }

    fun showRelatedToVideoId(videoId: String): Boolean =
            if (relatedToVideoId.value == videoId) false
            else {
                relatedToVideoId.value = videoId
                true
            }


    fun showSearchQuery(searchQuery: String): Boolean =
            if (queryString.value == searchQuery) false
            else {
                queryString.value = searchQuery
                true
            }


    fun retry(){
        val listing = searchResult?.value
        listing?.retry?.invoke()
    }

    fun currentVideoId(): String? = relatedToVideoId.value
}