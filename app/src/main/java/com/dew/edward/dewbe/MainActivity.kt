package com.dew.edward.dewbe


import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
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
import android.widget.Toast
import com.dew.edward.dewbe.adapter.VideoModelAdapter
import com.dew.edward.dewbe.model.NetworkState
import com.dew.edward.dewbe.model.VideoModel
import com.dew.edward.dewbe.ui.DewApp
import com.dew.edward.dewbe.ui.ExoVideoPlayActivity
import com.dew.edward.dewbe.ui.VideoPlayActivity
import com.dew.edward.dewbe.util.*
import com.dew.edward.dewbe.viewmodel.DbVideoViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.settings_dialog.*


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var videoViewModel: DbVideoViewModel
    private lateinit var preferences: SharedPreferences
    private lateinit var query: String
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        preferences = getPreferences(Context.MODE_PRIVATE)
        query = preferences.getString(KEY_QUERY, DEFAULT_QUERY)



        initActionBar()
        videoViewModel = DbVideoViewModel.getViewModel(this@MainActivity)
        initRecyclerView()
        initSwipeToRefresh()

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(KEY_QUERY)
        }
        Log.d(TAG, "onCreate, query = $query")
        videoViewModel.showSearchQuery(query)
    }

    override fun onResume() {
        super.onResume()
        hideKeyboard()
        searchView?.clearFocus()
    }

    private fun initActionBar() {
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.mipmap.ic_launcher)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initRecyclerView() {
        val glide = GlideApp.with(this)
        val adapter = VideoModelAdapter(
                { videoViewModel.retry() },
                {
                    val intent = if (DewApp.playerType == "YoutubePlayer"){
                        Intent(this@MainActivity, VideoPlayActivity::class.java)
                    } else {
                        Intent(this@MainActivity, ExoVideoPlayActivity::class.java)
                    }

                    intent.putExtra(VIDEO_MODEL, it)
                    startActivity(intent)
                }
        )

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mainListView.layoutManager = GridLayoutManager(this, 2)
        } else {
            mainListView.layoutManager = LinearLayoutManager(this)
        }

        mainListView.adapter = adapter
        mainListView.setHasFixedSize(true)
        videoViewModel.videoList.observe(this, Observer<PagedList<VideoModel>> {
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

        searchView = menu.findItem(R.id.menu_search).actionView as SearchView?

        val view: SearchView = searchView!!
        if (searchView != null) initSearchView(view)


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
                        if (videoViewModel.showSearchQuery(it)) {
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
        if (inputManager.isAcceptingText) {
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_settings -> {
                showDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDialog() {
        val view = layoutInflater.inflate(R.layout.settings_dialog, null)
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog)
                .setTitle("Settings")
                .setView(view)
                .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                    Toast.makeText(this, "Settings will be effective after the app restarts.", Toast.LENGTH_LONG).show()
                })
                .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                    Toast.makeText(this, "Negative button clicked.", Toast.LENGTH_SHORT).show()
                })
                .create()
        dialog.show()

        dialog.enableDB.setOnClickListener {
            for (i in 0 until dialog.dbTypeGroup.childCount) {
                dialog.dbTypeGroup.getChildAt(i).isEnabled = dialog.enableDB.isChecked
            }

            if (dialog.enableDB.isChecked){
                DewApp.sharedPreferences.edit().putBoolean(KEY_IS_DB_ENABLED, true).apply()
            } else {
                DewApp.sharedPreferences.edit().putBoolean(KEY_IS_DB_ENABLED, false).apply()
                DewApp.sharedPreferences.edit().putString(KEY_DB_TYPE, "").apply()
            }
//            Toast.makeText(this, "enableDb clicked", Toast.LENGTH_LONG).show()
        }

        dialog.playerGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId){
                R.id.youtubePlayer -> DewApp.sharedPreferences.edit().putString(KEY_PLAYER_TYPE, "YoutubePlayer").apply()
                R.id.exoplayer -> DewApp.sharedPreferences.edit().putString(KEY_PLAYER_TYPE, "ExoPlayer").apply()
            }
//            Toast.makeText(this, "playerGroup clicked", Toast.LENGTH_LONG).show()
        }

        dialog.dbTypeGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId){
                R.id.db_in_mem -> DewApp.sharedPreferences.edit().putString(KEY_DB_TYPE, "DB_In_Memory").apply()
                R.id.db_on_disk -> DewApp.sharedPreferences.edit().putString(KEY_DB_TYPE, "DB_On_Disk").apply()
            }
//            Toast.makeText(this, "dbTypeGroup clicked", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(KEY_QUERY, videoViewModel.currentQuery())
    }

    override fun onDestroy() {
        with(preferences.edit()) {
            putString(KEY_QUERY, query)
            apply()
        }

        super.onDestroy()
    }
}
