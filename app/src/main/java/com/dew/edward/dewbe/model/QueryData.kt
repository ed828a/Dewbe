package com.dew.edward.dewbe.model


/**
 * Created by Edward on 6/26/2018.
 */
data class QueryData (val query: String = "", val type: Type = Type.QUERY_STRING)
enum class Type{
    QUERY_STRING,
    RELATED_VIDEO_ID
}