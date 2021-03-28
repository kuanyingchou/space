package com.ken.space.model

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import com.ken.space.LaunchesDao
import com.ken.space.SpaceService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class LaunchesViewModel(private val launchesDao: LaunchesDao,
                        private val spaceService: SpaceService,
                        private val mainDispatcher: CoroutineContext,
                        private val ioDispatcher: CoroutineContext): ViewModel() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val _launches: Flow<List<Launch>> = launchesDao.getAll()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val filter: MutableStateFlow<String> = MutableStateFlow("")

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val _filteredLaunchesFlow: Flow<List<Launch>> = _launches
        .combine(filter) { a, b -> Pair(a, b) }
        .map { (list, filter) ->
            val now = DateTime.now()
            list.filter { match(it.mission?.name, filter) && it.net.isAfter(now) }
        }
        .distinctUntilChanged()

    val filteredLaunchesByDate: StateFlow<Map<String, List<Launch>>> = _filteredLaunchesFlow
        .map { launches ->
            launches.groupBy {
                DateTimeFormat
                    .mediumDate()
                    .withLocale(Locale.getDefault())
                    .print(it.net.withZone(DateTimeZone.getDefault()))
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, emptyMap())

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val error: MutableStateFlow<String> = MutableStateFlow("")

    private fun match(str: String?, keyword: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        if (keyword.isNullOrEmpty()) return true
        return str.toLowerCase(Locale.getDefault()).contains(keyword.toLowerCase(Locale.getDefault()))
    }

    fun updateDB() {
        if (isLoading.value) return
        viewModelScope.launch(mainDispatcher) {
            isLoading.value = true
            loadAndUpdate()
            isLoading.value = false
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun loadAndUpdate() {
        withContext(ioDispatcher) {
            try {
                val remoteLaunches = getRemoteLaunches()
                //Log.d("gyz", remoteLaunches.toString())
                updateLocalLaunches(remoteLaunches)
            } catch(e: Exception) {
                error.value = "Network Error"
            }
        }
    }

    fun updateFilter(filter: String) {
        this.filter.value = filter
    }

    fun dismissError() {
        error.value = ""
    }

    @WorkerThread
    private suspend fun getRemoteLaunches(): List<Launch> {
        return spaceService.getUpcomingLaunches().results
    }

    @WorkerThread
    private suspend fun updateLocalLaunches(remoteLaunches: List<Launch>) {
        launchesDao.insertAll(*remoteLaunches.toTypedArray())
    }
}