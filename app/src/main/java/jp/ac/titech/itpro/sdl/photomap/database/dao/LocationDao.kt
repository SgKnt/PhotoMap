package jp.ac.titech.itpro.sdl.photomap.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import jp.ac.titech.itpro.sdl.photomap.database.entity.Location

@Dao
interface LocationDao {
    @get:Query("SELECT * FROM location")
    val all: List<Location>

    @Insert
    fun insert(location: Location): Long

    @Delete
    fun delete(location: Location)
}