package jp.ac.titech.itpro.sdl.myapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Photo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        private var db: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase {
            if (db == null) {
                db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java, "photomap_db"
                    ).build()
            }
            return db!!
        }
    }
}