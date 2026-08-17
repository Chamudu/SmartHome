package com.smarthome.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.smarthome.app.ui.outlet.OutletRoute
import com.smarthome.app.ui.theme.SmartHomeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartHomeApp()
        }
    }
}

@Composable
fun SmartHomeApp() {
    SmartHomeTheme {
        OutletRoute()
    }
}
