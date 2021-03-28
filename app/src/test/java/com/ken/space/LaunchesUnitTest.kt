package com.ken.space

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ken.space.model.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.component.get
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.coroutines.CoroutineContext

class LaunchesUnitTest: KoinTest {

    init {
        // For java.io.IOException: Resource not found: "org/joda/time/tz/data/ZoneInfoMap"
        DateTimeZone.setProvider(UTCProvider())
    }


    val localLaunches = listOf(
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

    val remoteLaunches = listOf(
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

    val mergedLaunches = listOf(
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

    val ioDispatcher by inject<CoroutineContext>(named("io"))
    val mainDispatcher by inject<CoroutineContext>(named("main"))

    private val m = module(true, true) {

        single<Gson> {
            GsonBuilder()
                .registerTypeAdapter(DateTime::class.java,
                    DateTimeAdapter()
                )
                .create()
        }

        single<SpaceService> {
            mockk() {
                coEvery { getUpcomingLaunches() } returns get<UpcomingLaunchResult>()
            }
        }

        single<UpcomingLaunchResult> {
            mockk<UpcomingLaunchResult>() {
                every { results } returns remoteLaunches
            }
        }

        single<CoroutineContext>(named("main")) {
            TestCoroutineDispatcher()
        }

        single<CoroutineContext>(named("io")) {
            TestCoroutineDispatcher()
        }

        single<SpaceDB>() {
            mockk()
        }

        single<LaunchesDao>() {
            mockk() {
                coEvery { getAll() } returns flow {
                    emit(localLaunches)
                    delay(100)
                    emit(mergedLaunches)
                }
            }
        }

        viewModel<LaunchesViewModel> {
            LaunchesViewModel(
                launchesDao = get(),
                spaceService = get(),
                mainDispatcher = get(named("main")),
                ioDispatcher = get(named("io"))
            )
        }

        viewModel<DetailsViewModel> { params ->
            DetailsViewModel(launchId = params.get(), launchesDao = get())
        }

        single<CoroutineScope> {
            TestCoroutineScope()
        }
    }

    // For starting/stopping koin context
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger()
        modules(m)
    }

    @Before
    fun setup() {
        // Set the current time(for filtering)
        val nowMillis = DateTime.parse("2021-02-28T00:00:01.000Z").millis
        DateTimeUtils.setCurrentMillisFixed(nowMillis)
    }


    @Test
    fun test_initial_state() = runBlocking {
        val model = get<LaunchesViewModel>()

        advanceTimeBy(1000)

        assertEquals(localLaunches, model._launches.first())
        assertEquals("", model.filter.first())
        assertEquals(false, model.isLoading.first())
        assertEquals("", model.error.first())
    }

    @Test
    fun test_merge() = runBlocking {

        val model = get<LaunchesViewModel>()

        val launchesStateFlow = model._launches.flowOn(ioDispatcher).stateIn(get())

        assertEquals(localLaunches, launchesStateFlow.first())

        advanceTimeBy(1000)

        assertEquals(mergedLaunches, launchesStateFlow.first())

    }

    @Test
    fun test_filter() = runBlocking {
        val model = get<LaunchesViewModel>()

        val launchesStateFlow = model._launches.flowOn(ioDispatcher).stateIn(get())
        val filtered = model._filteredLaunchesFlow.flowOn(ioDispatcher).stateIn(get())

        assertEquals(localLaunches, launchesStateFlow.first())

        advanceTimeBy(1000)

        assertEquals(mergedLaunches, launchesStateFlow.first())

        model.updateFilter("nasa")

        advanceTimeBy(1000)

        assertEquals(
            listOf(mergedLaunches[1], mergedLaunches[3]),
            filtered.first()
        )

        model.updateFilter("spx")

        assertEquals(
            listOf(mergedLaunches[2]),
            filtered.first()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun advanceTimeBy(millis: Long) {
        (mainDispatcher as TestCoroutineDispatcher).advanceTimeBy(millis)
        (ioDispatcher as TestCoroutineDispatcher).advanceTimeBy(millis)
    }
}