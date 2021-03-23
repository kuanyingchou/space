package com.ken.space.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ken.space.R
import com.ken.space.model.LaunchesViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.viewmodel.ext.android.viewModel

class LaunchesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launches)

        val model = getViewModel()

        if(savedInstanceState==null) {
            model.updateDB()
        }

        val recyclerView = setupRecyclerView(model)

        setupBackToTopButton(model, recyclerView)

        val filterEditText = setupFilter(model)

        setupClearButton(model, filterEditText)

        setupErrorToast(model)

        setupSwipeRefresh(model)

        setupEmptyView(model, recyclerView)
    }

    private fun getViewModel(): LaunchesViewModel {
        val model by viewModel<LaunchesViewModel>()
        return model
    }

    private fun setupRecyclerView(model: LaunchesViewModel): RecyclerView {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val lm = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        recyclerView.apply {
            layoutManager = lm
            adapter = LaunchesAdapter(this@LaunchesActivity, model)
        }

        recyclerView.addOnScrollListener(object: OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                model.setFirstVisibleItemPosition(lm.findFirstVisibleItemPosition())
            }
        })

        model.filteredLaunches.observe(this, Observer {
            model.setFirstVisibleItemPosition(lm.findFirstVisibleItemPosition())
        })

        return recyclerView
    }

    private fun setupBackToTopButton(model: LaunchesViewModel, recyclerView: RecyclerView) {
        val backToTopButton = findViewById<Button>(R.id.back_to_top_button)
        model.isAtTop.observe(this, Observer { isAtTop ->
            backToTopButton.visibility = if(isAtTop) View.GONE else View.VISIBLE
        })
        backToTopButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0) // no round trip
        }
    }

    private fun setupFilter(model: LaunchesViewModel): EditText {
        val filterEditText = findViewById<EditText>(R.id.filter_edit_text)
        filterEditText.doAfterTextChanged {
            model.updateFilter(it.toString())
        }
        return filterEditText
    }

    private fun setupClearButton(model: LaunchesViewModel, filterEditText: EditText) {
        val clearButton = findViewById<Button>(R.id.clear_button)
        model.filter.observe(this, Observer {
            clearButton.isEnabled = it.isNotEmpty()
        })

        clearButton.setOnClickListener {
            filterEditText.text.clear() // no trip to view model to prevent infinite loop
        }
    }

    private fun setupErrorToast(model: LaunchesViewModel) {
        model.error.observe(this, Observer {
            if(it.isNotEmpty()) {
                Toast.makeText(this@LaunchesActivity, it, Toast.LENGTH_SHORT).show()
                model.dismissError()
            }
        })
    }

    private fun setupSwipeRefresh(model: LaunchesViewModel) {
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh).apply {
            setOnRefreshListener {
                model.updateDB()
            }
        }
        model.isLoading.observe(this, Observer {isLoading ->
            swipeRefresh.isRefreshing = isLoading
        })
    }

    private fun setupEmptyView(model: LaunchesViewModel, recyclerView: RecyclerView) {
        val emptyView = findViewById<TextView>(R.id.empty_view)
        model.filteredLaunches.observe(this, Observer {
            emptyView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
        })
    }
}