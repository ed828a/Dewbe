package com.dew.edward.dewbe.model


/**
 * Created by Edward on 6/26/2018.
 */
data class YoutubeResponseData(val prevPageToken: String,
                               val nextPageToken: String,
                               val pageInfo: PageInfo,
                               val items: List<Item>) {
    data class PageInfo(val totalResults: String,
                        val resultsPerPage: String)

    data class Item(val id: ID,
                    val snippet: Snippet) {
        data class ID(val kind: String,
                      val videoId: String)

        data class Snippet(val publishedAt: String,
                           val title: String,
                           val thumbnails: Thumbnails) {
            data class Thumbnails(val high: High) {
                class High(val url: String)
            }
        }
    }
}