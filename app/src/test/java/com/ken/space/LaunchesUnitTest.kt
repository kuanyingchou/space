package com.ken.space

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ken.space.model.Launch
import com.ken.space.model.LaunchesViewModel
import com.ken.space.model.Mission
import com.ken.space.model.UpcomingLaunchResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.Before
import org.junit.Test

import org.junit.Rule

class LaunchesUnitTest {

    // For testing LiveData
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mainDispatcher = TestCoroutineDispatcher()
    private val ioDispatcher = TestCoroutineDispatcher()

    private lateinit var spaceService: SpaceService
    private lateinit var launchesDao: LaunchesDao

    private lateinit var localLaunches: List<Launch>
    private lateinit var remoteLaunches: List<Launch>
    private lateinit var mergedLaunches: List<Launch>

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)

        val context: Context = mockk()

        // For java.io.IOException: Resource not found: "org/joda/time/tz/data/ZoneInfoMap"
        DateTimeZone.setProvider(UTCProvider())

        // Set the current time
        val nowMillis = DateTime.parse("2021-02-28T00:00:01.000Z").millis
        DateTimeUtils.setCurrentMillisFixed(nowMillis)

        spaceService = mockk()
        launchesDao = mockk()

        localLaunches = listOf(
                mockk<Launch>(relaxed = true){
                    every { id } returns "a"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "spx"
                    }
                    every { net } returns DateTime.parse("2021-02-28T00:00:00.000Z")
                },
                mockk<Launch>(relaxed = true) {
                    every { id } returns "b"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "spx"
                    }
                    every { net } returns DateTime.parse("2021-03-01T00:00:00.000Z")
                },
                mockk<Launch>(relaxed = true) {
                    every { id } returns "c"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "nasa"
                    }
                    every { net } returns DateTime.parse("2021-03-01T00:00:00.000Z")
                }
        )

        remoteLaunches = listOf(
                mockk<Launch>(relaxed = true) {
                    every { id } returns "b"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "spx"
                    }
                    every { net } returns DateTime.parse("2021-03-07T00:00:00.000Z")
                },
                mockk<Launch>(relaxed = true) {
                    every { id } returns "d"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "nasa"
                    }
                    every { net } returns DateTime.parse("2021-03-08T00:00:00.000Z")
                }
        )

        mergedLaunches = listOf(
                mockk<Launch>(relaxed = true){
                    every { id } returns "a"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "spx"
                    }
                    every { net } returns DateTime.parse("2021-02-28T00:00:00.000Z")
                },
                mockk<Launch>(relaxed = true) {
                    every { id } returns "c"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "nasa"
                    }
                    every { net } returns DateTime.parse("2021-03-01T00:00:00.000Z")
                },
                mockk<Launch>(relaxed = true) {
                    every { id } returns "b"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "spx"
                    }
                    every { net } returns DateTime.parse("2021-03-07T00:00:00.000Z")
                },
                mockk<Launch>(relaxed = true) {
                    every { id } returns "d"
                    every { mission } returns mockk<Mission> {
                        every { name } returns "nasa"
                    }
                    every { net } returns DateTime.parse("2021-03-08T00:00:00.000Z")
                }
        )

        coEvery { launchesDao.getAll() } returns flow {
            emit(localLaunches)
            delay(100)
            emit(mergedLaunches)
        }.flowOn(ioDispatcher)

        val remoteResult = mockk<UpcomingLaunchResult>()
        every { remoteResult.results } returns remoteLaunches

        coEvery { spaceService.getUpcomingLaunches() } returns remoteResult
    }


    @Test
    fun test_initial_state() = runBlocking<Unit> {
        val model = LaunchesViewModel(launchesDao, spaceService, mainDispatcher, ioDispatcher)

        assertEquals(localLaunches, model._launches.first())
        assertEquals("", model.filter.first())
        assertEquals(false, model.isLoading.first())
        assertEquals("", model.error.first())
    }

    @Test
    fun test_merge() = runBlocking<Unit> {
        val model = LaunchesViewModel(launchesDao, spaceService, mainDispatcher, ioDispatcher)

        model.loadAndUpdate()

        mainDispatcher.advanceTimeBy(1000)
        ioDispatcher.advanceTimeBy(1000)

        assertEquals(mergedLaunches, model._launches.first())

    }

    @Test
    fun test_filter() = runBlocking<Unit> {
        val model = LaunchesViewModel(launchesDao, spaceService, mainDispatcher, ioDispatcher)

        model.loadAndUpdate()

        mainDispatcher.advanceTimeBy(1000)
        ioDispatcher.advanceTimeBy(1000)

        assertEquals(mergedLaunches, model._launches.first())

        model.updateFilter("nasa")

        mainDispatcher.advanceTimeBy(1000)
        ioDispatcher.advanceTimeBy(1000)

        assertEquals(
                listOf(mergedLaunches[1], mergedLaunches[3]),
                model._filteredLaunchesFlow.value
        )

        model.updateFilter("spx")

        mainDispatcher.advanceTimeBy(1000)
        ioDispatcher.advanceTimeBy(1000)

        assertEquals(
                listOf(mergedLaunches[2]),
                model._filteredLaunchesFlow.value
        )
    }
}