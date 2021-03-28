package com.ken.space

import androidx.room.*
import com.ken.space.model.Converters
import com.ken.space.model.Launch
import kotlinx.coroutines.flow.Flow

@Database(entities = [Launch::class], version = 1)
@TypeConverters(Converters::class)
abstract class SpaceDB : RoomDatabase() {
    abstract fun launchesDao(): LaunchesDao
}

@Dao
interface LaunchesDao {

    @Query("SELECT * FROM launch ORDER BY datetime(net)")
    fun getAll(): Flow<List<Launch>> // Use Flow here to let the client listen for updates

    @Query("SELECT * FROM launch WHERE id = :id")
    suspend fun get(id: String): Launch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg launches: Launch): List<Long>
}
