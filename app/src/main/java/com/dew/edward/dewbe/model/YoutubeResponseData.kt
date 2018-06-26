package com.dew.edward.dewbe.model


/**
 * Created by Edward on 6/26/2018.
 */
class YoutubeResponseData(val prevPageToken: String,
                          val nextPageToken: String,
                          val pageInfo: PageInfo,
                          val items: List<Item>) {
    class PageInfo(val totalResults: String,
                   val resultsPerPage: String)

    class Item(val id: ID,
               val snippet: Snippet){
        class  ID(val kind: String,
                  val videoId: String)

        class Snippet(val publishedAt: String,
                      val title: String,
                      val thumbnails: Thumbnails){
            class Thumbnails(val high: High){
                class High(val url: String)
            }
        }
    }
}