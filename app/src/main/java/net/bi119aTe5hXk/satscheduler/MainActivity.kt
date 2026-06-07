package net.bi119aTe5hXk.satscheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.bi119aTe5hXk.satscheduler.ui.SatSchedulerApp
import net.bi119aTe5hXk.satscheduler.ui.theme.SatSchedulerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SatSchedulerTheme {
                SatSchedulerApp()
            }
        }
    }
}
