package dev.ericferguson.watertracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.ericferguson.watertracker.ui.TodayScreen
import dev.ericferguson.watertracker.ui.theme.WaterTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WaterTrackerTheme {
                TodayScreen()
            }
        }
    }
}
