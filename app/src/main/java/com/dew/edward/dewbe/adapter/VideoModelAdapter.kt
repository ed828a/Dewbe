package com.dew.edward.dewbe.adapter

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dew.edward.dewbe.R
import com.dew.edward.dewbe.model.NetworkState
import com.dew.edward.dewbe.model.VideoModel
import com.dew.edward.dewbe.util.GlideApp
import com.dew.edward.dewbe.util.GlideRequests
import kotlinx.android.synthetic.main.cell_video.view.*

/**
 * Created by Edward on 6/26/2018.
 */
class VideoModelAdapter(private val retryCallback: () -> Unit,
                        val listener: (VideoModel) -> Unit) : PagedListAdapter<VideoModel, RecyclerView.ViewHolder>(COMPARATOR) {

    private var networkState: NetworkState? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.cell_video -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.cell_video, parent, false)
                //for test now
                val glideRequests = GlideApp.with(parent.context)
                VideoModelViewHolder(view, glideRequests)
            }
            R.layout.cell_network_state -> NetworkStateItemViewHolder.create(parent, retryCallback)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.cell_video -> {
                val item = getItem(position)
                if (item != null) {
                    (holder as VideoModelViewHolder).bind(item)
                    holder.setOnItemSelectedListener(getItem(position)!!)
                }
            }
            R.layout.cell_network_state -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }
    }

    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.cell_network_state
        } else {
            R.layout.cell_video
        }
    }

    override fun getItemCount(): Int {
        return (super.getItemCount() + if (hasExtraRow()) 1 else 0)
    }

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }


    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<VideoModel>() {
            override fun areItemsTheSame(oldItem: VideoModel?, newItem: VideoModel?): Boolean {
                return oldItem?.videoId == newItem?.videoId
            }

            override fun areContentsTheSame(oldItem: VideoModel?, newItem: VideoModel?): Boolean {
                return oldItem?.title == newItem?.title
            }

            override fun getChangePayload(oldItem: VideoModel, newItem: VideoModel): Any? {
                return if (oldItem.copy(title = newItem.title) == newItem) {
                    Any()
                } else {
                    null
                }
            }
        }
    }

    inner class VideoModelViewHolder(view: View,
                                     private val glide: GlideRequests) : RecyclerView.ViewHolder(view) {

        private val textViewTitle = view.textViewTitle
        private val textViewDesc = view.textViewChannelTitle
        private val textViewDate = view.textViewDate
        private val imageViewThumb = view.imageViewThumb

        fun setOnItemSelectedListener(videoModel: VideoModel) {
            itemView.setOnClickListener {
                listener(videoModel)
            }
        }

        fun bind(videoModel: VideoModel) {
            textViewTitle.text = videoModel.title
            textViewDate.text = videoModel.date
            glide.load(videoModel.thumbnail).centerCrop().into(imageViewThumb)
        }
    }
}