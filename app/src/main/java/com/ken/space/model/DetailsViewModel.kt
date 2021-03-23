package com.ken.space.model

import androidx.lifecycle.*
import com.ken.space.LaunchesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder

// "T-dd*:hh:mm:ss"
private val periodFormatter: PeriodFormatter = PeriodFormatterBuilder()
    .appendLiteral("T-")
    .printZeroAlways()
    .minimumPrintedDigits(2)
    .appendDays()
    .appendSeparator(":")
    .appendHours()
    .appendSeparator(":")
    .appendMinutes()
    .appendSeparator(":")
    .appendSeconds()
    .toFormatter()

class DetailsViewModel(launchId: String, launchesDao: LaunchesDao): ViewModel() {

    private val _launch: MutableStateFlow<Launch?> = MutableStateFlow(null)
    val launch: LiveData<Launch> = _launch
            .filterNotNull()
            .asLiveData(Dispatchers.Main)

    private val _countdown: Flow<String> = _launch.filterNotNull().flatMapLatest {
        flow {
            val periodType = PeriodType.dayTime().withMillisRemoved()
            var period = Period.ZERO

            while(true) {
                val now = DateTime.now()
                if(it.net.isAfter(now)) {
                    val p = Period(now, it.net, periodType)
                    if (p != period) {
                        period = p
                    }
                } else {
                    period = Period.ZERO
                }
                emit(period.toString(periodFormatter))
                delay(100)
            }
        }
    }
    val countdown = _countdown.asLiveData(Dispatchers.Main)

    init {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                _launch.value = launchesDao.get(launchId)!!
            }
        }
    }

    class Factory(private val launchId: String, private val launchesDao: LaunchesDao): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return DetailsViewModel(launchId, launchesDao) as T
        }
    }
}