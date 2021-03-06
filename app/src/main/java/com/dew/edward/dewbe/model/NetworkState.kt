package com.dew.edward.dewbe.model


/**
 * Created by Edward on 6/26/2018.
 */
enum class Status {
    RUNNING,
    SUCCESS,
    FAILED
}

@Suppress("DataClassPrivateConstructor")
data class  NetworkState private constructor(val status: Status, val msg: String? = null){
    companion object {
        val LOADED = NetworkState(Status.SUCCESS)
        val LOADING = NetworkState(Status.RUNNING)
        fun error(msg: String?) = NetworkState(Status.FAILED, msg)
    }
}