package jp.ac.titech.itpro.sdl.photomap.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

import java.util.Date

@Entity
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "photo_uri")
    val photoURI: String,
    val locationId: Long,
    val memo: String,
    val date: Date
)