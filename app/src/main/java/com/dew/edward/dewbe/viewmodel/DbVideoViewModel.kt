package com.dew.edward.dewbe.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.dew.edward.dewbe.repository.DbVideoModelRepository


/**
 * Created by Edward on 6/26/2018.
 */
class DbVideoViewModel(context: Context): ViewModel() {
    val repository = DbVideoModelRepository(context)

    private val queryString = MutableLiveData<String>()
    private val searchResult =
            Transformations.map(queryString) {queryString ->
                with(repository) {
                    resetPageStatus()
                    searchVideosOnYoutube(queryString)
                }
            }
    val posts = Transformations.switchMap(searchResult) { it.pagedList }!!
    val networkState = Transformations.switchMap(searchResult) { it.networkState }!!
    val refreshState = Transformations.switchMap(searchResult) { it.refreshState }!!

    fun refresh() {
        searchResult.value?.refresh?.invoke()
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

    fun currentQuery(): String? = queryString.value
}