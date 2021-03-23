package com.ken.space.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat

private val FORMATTER = ISODateTimeFormat.dateTime()

data class UpcomingLaunchResult(
    val count: Int,
    val results: List<Launch>
)

@Entity
data class Launch(
    @PrimaryKey
    val id: String,
    val url: String,
    val name: String,
    val slug: String,
    val image: String?,
    val net: DateTime, // No Earlier Than
    @Embedded(prefix = "mission_")
    val mission: Mission?,
    @Embedded(prefix = "pad_")
    val pad: Pad?,
    @Embedded(prefix = "agency_")
    val launch_service_provider: Agency?,
    @Embedded(prefix = "status_")
    val status: LaunchStatus,
    val infoURLs: String?,
    val vidURLs: String?,
)

data class Mission(
    val id: String,
    val name: String,
    val description: String
)

data class Pad(
    val id: String,
    val name: String,
    val latitude: String,
    val longitude: String
)

data class Agency(
    val id: String,
    val name: String
)

data class LaunchStatus(
    val id: String,
    val name: String
)

// For Gson
class DateTimeAdapter: TypeAdapter<DateTime>() {
    override fun write(writer: JsonWriter?, value: DateTime?) {
        writer?.value(value?.toString(FORMATTER))
    }

    override fun read(reader: JsonReader?): DateTime {
        return DateTime.parse(reader?.nextString())
    }
}

// For Room
class Converters {
    @TypeConverter
    fun fromDateTime(dateTime: DateTime?): String? {
        // Always save UTC time in case timezone on device changes
        return dateTime?.withZone(DateTimeZone.UTC).toString()
    }

    @TypeConverter
    fun toDateTime(value: String?): DateTime? {
        return value?.let { DateTime.parse(it, FORMATTER) }
    }
}