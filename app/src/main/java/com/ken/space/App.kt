package com.ken.space

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ken.space.model.DateTimeAdapter
import com.ken.space.model.DetailsViewModel
import com.ken.space.model.LaunchesViewModel
import kotlinx.coroutines.Dispatchers
import org.joda.time.DateTime
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.coroutines.CoroutineContext

class App: Application() {

    private val appModule = module {

        single<SpaceDB> {
            Room.databaseBuilder(
                get(),
                SpaceDB::class.java,
                "space"
            ).build()
        }

        single<Gson> {
            GsonBuilder()
                    .registerTypeAdapter(DateTime::class.java,
                            DateTimeAdapter()
                    )
                    .create()
        }

        single<SpaceService> {
            Retrofit.Builder()
                    .baseUrl(DEBUG_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(get()))
                    .build().create(SpaceService::class.java)
        }

        single<CoroutineContext>(named("main")) {
            Dispatchers.Main
        }

        single<CoroutineContext>(named("io")) {
            Dispatchers.IO
        }

        single<LaunchesDao>() {
            get<SpaceDB>().launchesDao()
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
    }

    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
    }
}