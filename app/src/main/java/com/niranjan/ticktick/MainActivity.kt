package com.niranjan.ticktick

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.niranjan.ticktick.app.TickTickApp
import com.niranjan.ticktick.app.TickTickApplication
import com.niranjan.ticktick.core.designsystem.TickTickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            TickTickTheme {
                TickTickApp((application as TickTickApplication).container)
            }
        }
    }
}
