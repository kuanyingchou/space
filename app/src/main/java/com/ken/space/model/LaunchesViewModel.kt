package com.ken.space.model

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.lifecycle.*
import com.ken.space.LaunchesDao
import com.ken.space.SpaceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // An alternative is to put the Flows in a Repo class and keep the LiveData here, but that
    // introduces another layer. And we have no need to access these Flows in other ViewModels
    // for now.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val _launches: Flow<List<Launch>> = launchesDao.getAll()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val _filter: MutableStateFlow<String> = MutableStateFlow("")

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val _filteredLaunchesFlow: Flow<List<Launch>> = _launches
        .combine(_filter) { a, b -> Pair(a, b) }
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
    val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: LiveData<Boolean> = _isLoading
            //.distinctUntilChanged() // this has no effect on StateFlow: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/index.html
            .asLiveData(mainDispatcher)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val _error: MutableStateFlow<String> = MutableStateFlow("")
    val error: LiveData<String> = _error.asLiveData(mainDispatcher)

    val _firstVisibleItemPosition: MutableStateFlow<Int> = MutableStateFlow(0)
    val isAtTop: LiveData<Boolean> = _firstVisibleItemPosition
            .map { it == 0 }
            .asLiveData(mainDispatcher)

    private fun match(str: String?, keyword: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        if (keyword.isNullOrEmpty()) return true
        return str.toLowerCase(Locale.getDefault()).contains(keyword.toLowerCase(Locale.getDefault()))
    }

    fun updateDB() {
        if (_isLoading.value) return
        viewModelScope.launch(mainDispatcher) {
            _isLoading.value = true
            loadAndUpdate()
            _isLoading.value = false
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
                _error.value = "Network Error"
            }
        }
    }

    fun updateFilter(filter: String) {
        _filter.value = filter
    }

    fun dismissError() {
        _error.value = ""
    }

    fun setFirstVisibleItemPosition(position: Int) {
        _firstVisibleItemPosition.value = position
    }

    @WorkerThread
    private suspend fun getRemoteLaunches(): List<Launch> {
        return spaceService.getUpcomingLaunches().results
    }

    @WorkerThread
    private suspend fun updateLocalLaunches(remoteLaunches: List<Launch>) {
        launchesDao.insertAll(*remoteLaunches.toTypedArray())
    }

    class LaunchesViewModelFactory(
        private val launchesDao: LaunchesDao,
        private val spaceService: SpaceService,
        private val mainDispatcher: CoroutineContext,
        private val ioDispatcher: CoroutineContext
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LaunchesViewModel(launchesDao, spaceService, mainDispatcher, ioDispatcher) as T
        }
    }
}