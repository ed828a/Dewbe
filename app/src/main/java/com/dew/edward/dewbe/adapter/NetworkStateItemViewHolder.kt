package com.dew.edward.dewbe.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dew.edward.dewbe.R
import com.dew.edward.dewbe.model.NetworkState
import com.dew.edward.dewbe.model.Status
import kotlinx.android.synthetic.main.cell_network_state.view.*


/**
 * Created by Edward on 6/26/2018.
 */
class NetworkStateItemViewHolder(view: View, private val retryCallback: () -> Unit) : RecyclerView.ViewHolder(view) {
    private val progressBar = view.progress_bar
    private val buttonRetry = view.retry_button
    private val textError = view.error_msg

    init {
        buttonRetry.setOnClickListener {
            retryCallback()
        }
    }

    fun bindTo(networkState: NetworkState?) {
        progressBar.visibility = toVisibility(networkState?.status == Status.RUNNING)
        buttonRetry.visibility = toVisibility(networkState?.status == Status.FAILED)
        textError.visibility = toVisibility(networkState?.msg != null)
        textError.text = networkState?.msg
    }

    companion object {
        fun create(parent: ViewGroup, retryCallback: () -> Unit): NetworkStateItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_network_state, parent, false)

            return NetworkStateItemViewHolder(view, retryCallback)
        }

        fun toVisibility(constraint: Boolean) = if (constraint) View.VISIBLE else View.GONE
    }
}