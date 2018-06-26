package com.dew.edward.dewbe.model

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList

/**
 * Created by Edward on 6/26/2018.
 */
data class Listing<T> (

        val pagedList: LiveData<PagedList<T>>,          // the LiveData of paged lists for the UI to observe

        val networkState: LiveData<NetworkState>,       // represents the network request status to show to the user

        val refreshState: LiveData<NetworkState>,       // represents the refresh status to show to the user, Separate from networkState,
                                                        // this value is importantly only when refresh is requested

        val refresh: () -> Unit,                        // refreshes the whole data and fetches it from scratch.

        val retry: () -> Unit                           // retries any failed requests
)