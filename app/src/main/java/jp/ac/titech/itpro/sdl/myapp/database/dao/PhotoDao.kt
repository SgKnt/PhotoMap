package jp.ac.titech.itpro.sdl.myapp.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import jp.ac.titech.itpro.sdl.myapp.database.entity.Photo

@Dao
interface PhotoDao {
    @get:Query("SELECT * FROM photo")
    val all: List<Photo>

    @Insert
    fun insert(photo: Photo): Long

    @Delete
    fun delete(photo: Photo)
}