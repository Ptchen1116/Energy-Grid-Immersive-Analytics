package com.ucl.energygrid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ucl.energygrid.ui.screen.MainScreen
import com.ucl.energygrid.data.repository.UserRepository
import com.ucl.energygrid.data.remote.apis.RetrofitInstance

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("UncaughtException", "Error in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        AppEnvironment.isDebug = true

        setContent {
            MainScreen(userRepository = UserRepository(RetrofitInstance.userApi))
        }
    }
}



