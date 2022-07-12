package jp.ac.titech.itpro.sdl.myapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

import java.util.Date

@Entity
class Photo(id: Int, photoURI: String, latitude: Float, longitude: Float, date: Date) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = id

    @ColumnInfo(name = "photo_uri")
    var photoURI: String = photoURI

    var latitude: Float = latitude
    var longitude: Float = longitude
    var date: Date = date
}