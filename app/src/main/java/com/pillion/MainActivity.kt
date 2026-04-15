package com.pillion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.pillion.navigation.PillionNavHost
import com.pillion.ui.theme.PillionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as PillionApplication).container

        setContent {
            PillionTheme {
                Surface {
                    PillionNavHost(container = container)
                }
            }
        }
    }
}
