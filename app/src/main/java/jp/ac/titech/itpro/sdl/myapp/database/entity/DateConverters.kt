package jp.ac.titech.itpro.sdl.myapp.database.entity

import androidx.room.TypeConverter
import java.util.Date

class DateConverters {
    @TypeConverter
    fun longToDate(date: Long): Date {
        return Date(date)
    }

    @TypeConverter
    fun dateToLong(date: Date): Long {
        return date.time
    }
}