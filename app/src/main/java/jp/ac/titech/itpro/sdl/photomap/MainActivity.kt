package jp.ac.titech.itpro.sdl.photomap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import jp.ac.titech.itpro.sdl.photomap.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        Log.d(TAG, "Start app")
    }

    companion object {
        private const val TAG = "Main Activity"
    }
}