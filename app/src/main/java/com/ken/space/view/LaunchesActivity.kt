package com.ken.space.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import com.bumptech.glide.request.RequestOptions
import com.google.accompanist.glide.GlideImage
import com.ken.space.SpaceTheme
import com.ken.space.model.Launch
import com.ken.space.model.LaunchesViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

class LaunchesActivity : AppCompatActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model by viewModel<LaunchesViewModel>()

        if(savedInstanceState==null) {
            model.updateDB()
        }

        setContent {
            SpaceTheme.Scaffold(title = "Upcoming Launches") {
                val launches = model.filteredLaunchesByDate.collectAsState(emptyMap()).value
                val filter = model.filter.collectAsState().value
                val isLoading = model.isLoading.collectAsState().value
                Column {
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    FilterAndLaunches(launches, filter, model)
                }
            }
        }

        setupErrorToast(model)

    }

    @ExperimentalAnimationApi
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun FilterAndLaunches(grouped: Map<String, List<Launch>>, filter: String, model: LaunchesViewModel) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        Box() {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Filter(filter, model)
                }

                for((date, launches) in grouped) {
                    stickyHeader {
                        Text(
                            text = date,
                            Modifier
                                .background(Color.White)
                                .padding(horizontal = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                    items(launches) { launch ->
                        Launch(launch)
                    }
                }
            }

            val showBackToTopButton = listState.firstVisibleItemIndex > 0

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                visible = showBackToTopButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.scrollToItem(0)
                        }
                    },
                    backgroundColor = Color.White
                ) {
                    Icon(Icons.Filled.North, contentDescription = null)
                }
            }

        }
    }

    @Composable
    private fun Filter(filter: String, model: LaunchesViewModel) {
        Box() {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = filter,
                onValueChange = { value ->
                    model.updateFilter(value)
                },
                placeholder = { Text(text = "Filter by Mission") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                maxLines = 1
            )
            if(filter.isNotEmpty()) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { model.updateFilter("") }
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                }
            }
        }
    }

    @Composable
    private fun Launch(launch: Launch) {
        val context = LocalContext.current

        Card(
            elevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(context, DetailsActivity::class.java)
                    intent.putExtra(KEY_LAUNCH_ID, launch.id)
                    startActivity(intent)
                }
                .padding(start = 8.dp, end = 8.dp)
        ) {
            Row(modifier = Modifier.wrapContentHeight()) {
                GlideImage(
                    modifier = Modifier.size(82.dp, 128.dp),
                    data = launch.image ?: "",
                    contentDescription = "",
                    fadeIn = true,
                    requestBuilder = {
                        val options = RequestOptions()
                        options.centerCrop()
                        apply(options)
                    }
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = launch.name,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = launch.launch_service_provider?.name ?: "",
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = launch.pad?.name ?: "",
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = DateTimeFormat
                            .forStyle("MM")
                            .withLocale(Locale.getDefault())
                            .print(launch.net.withZone(DateTimeZone.getDefault())),
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }

    private fun setupErrorToast(model: LaunchesViewModel) {
        addRepeatingJob(Lifecycle.State.STARTED) {
            model.error.collect {
                if (it.isNotEmpty()) {
                    Toast.makeText(this@LaunchesActivity, it, Toast.LENGTH_SHORT).show()
                    model.dismissError()
                }
            }
        }
    }

}