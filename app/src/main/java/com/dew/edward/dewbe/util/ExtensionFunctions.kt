package com.dew.edward.dewbe.util

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.dew.edward.dewbe.model.NetworkState


/**
 * Created by Edward on 6/26/2018.
 */

fun String.dbquery(): String {
    val strings = this.split(" ")
    var stringA = "%${strings[0]}%"
    if (strings.size > 1){
        for (i in 1 until strings.size){
            stringA = "$stringA | %${strings[i]}%"
        }
    }

    return stringA
}

fun String.extractDate(): String {
    val stringArray = this.split('T')

    return stringArray[0]
}

private fun getErrorMessage(report: PagingRequestHelper.StatusReport): String {
    return PagingRequestHelper.RequestType.values().mapNotNull {
        report.getErrorFor(it)?.message
    }.first()
}

fun PagingRequestHelper.createStatusLiveData(): LiveData<NetworkState> {
    val liveData = MutableLiveData<NetworkState>()
    addListener { report ->
        //when can also be used as a replacement for an if-else if chain. If no argument is supplied,
        // the branch conditions are simply boolean expressions,
        // and a branch is executed when its condition is true:
        when {
            report.hasRunning() -> liveData.postValue(NetworkState.LOADING)
            report.hasError() -> liveData.postValue(
                    NetworkState.error(getErrorMessage(report)))
            else -> liveData.postValue(NetworkState.LOADED)
        }
    }
    return liveData
}
