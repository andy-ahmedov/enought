package com.andyahmedov.enought.data.db

import androidx.room.TypeConverter
import java.time.Instant

class InstantTypeConverters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}

