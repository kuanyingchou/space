package com.ken.space

import com.google.gson.*
import com.ken.space.model.DateTimeAdapter
import com.ken.space.model.UpcomingLaunchResult
import org.joda.time.DateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// I hit the limit sometimes during development. The actual limit should be far less than the 300
// requests per day as claimed on the website.
const val PROD_BASE_URL = "https://ll.thespacedevs.com/2.0.0/"

// The data is stale, but it seems that there's no limit with this one.
const val DEBUG_BASE_URL = "https://lldev.thespacedevs.com/2.0.0/"

interface SpaceService {
    @GET("launch/upcoming")
    suspend fun getUpcomingLaunches(
        @Query("limit") limit: Int = 100, // took some liberty here :)
        @Query("offset") offset: Int = 0,
        @Query("ordering") ordering: String = "net",
        @Query("search") search: String? = null,
    ): UpcomingLaunchResult
}
