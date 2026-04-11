package com.mallar.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.mallar.app.navigation.MallARNavGraph
import com.mallar.app.ui.theme.MallARTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install the API
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)


        splashScreen.setOnExitAnimationListener { splashProvider ->
            splashProvider.remove()
        }

        enableEdgeToEdge()

        setContent {
            MallARTheme {
                // Your Compose Navigation starts here
                val navController = rememberNavController()
                MallARNavGraph(navController = navController)
            }
        }
    }
}