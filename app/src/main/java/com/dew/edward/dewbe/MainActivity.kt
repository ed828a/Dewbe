package com.dew.edward.dewbe


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.arch.paging.PagedList
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.dew.edward.dewbe.adapter.VideoModelAdapter
import com.dew.edward.dewbe.model.NetworkState
import com.dew.edward.dewbe.model.VideoModel
import com.dew.edward.dewbe.ui.VideoPlayActivity
import com.dew.edward.dewbe.util.DEFAULT_QUERY
import com.dew.edward.dewbe.util.GlideApp
import com.dew.edward.dewbe.util.KEY_QUERY
import com.dew.edward.dewbe.util.VIDEO_MODEL
import com.dew.edward.dewbe.viewmodel.DbVideoViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var videoViewModel: DbVideoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initActionBar()
        videoViewModel = getViewModel()
        initRecyclerView()
        initSwipeToRefresh()

        val query = savedInstanceState?.getString(KEY_QUERY) ?: DEFAULT_QUERY
        videoViewModel.showSearchQuery(query)
    }

    private fun initActionBar() {
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.mipmap.ic_launcher)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun getViewModel(): DbVideoViewModel =
            ViewModelProviders.of(this, object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                        DbVideoViewModel(this@MainActivity) as T
            })[DbVideoViewModel::class.java]

    private fun initRecyclerView() {
        val glide = GlideApp.with(this)
        val adapter = VideoModelAdapter(glide, { videoViewModel.retry() }, {
            val intent = Intent(this@MainActivity, VideoPlayActivity::class.java)

            intent.putExtra(VIDEO_MODEL, it)
            startActivity(intent)
        })

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mainListView.layoutManager = GridLayoutManager(this, 2)
        } else {
            mainListView.layoutManager = LinearLayoutManager(this)
        }

        mainListView.adapter = adapter
        mainListView.setHasFixedSize(true)
        videoViewModel.posts.observe(this, Observer<PagedList<VideoModel>> {
            adapter.submitList(it)
        })
        videoViewModel.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })
    }

    private fun initSwipeToRefresh() {
        videoViewModel.refreshState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        })
        swipeRefreshLayout.setOnRefreshListener {
            videoViewModel.refresh()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchView: SearchView? = menu.findItem(R.id.menu_search).actionView as SearchView?
        if (searchView != null) initSearchView(searchView)

        return true
    }

    private fun initSearchView(searchView: SearchView) {
        searchView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        searchView.layoutParams = ActionBar.LayoutParams(Gravity.END)
        searchView.queryHint = "Search Movie ..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.trim()?.let {
                    if (it.isNotEmpty()) {
//                        Toast.makeText(searchView.context, "searchView is clicked : ${query?.trim()}", Toast.LENGTH_SHORT).show()
                        if (videoViewModel.showSearchQuery(it)){
                            mainListView.scrollToPosition(0)
                            (mainListView.adapter as? VideoModelAdapter)?.submitList(null)
                        }
                    }
                }

                hideKeyboard()
                searchView.clearFocus()
                searchView.setQuery("", false)
                Log.d("initSearchView", "queryString: ${query?.trim()}")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun hideKeyboard() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputManager.isAcceptingText){
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.menu_search -> true
            R.id.menu_in_memory -> true
            R.id.menu_in_Storage -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(KEY_QUERY, videoViewModel.currentQuery())
    }
}
