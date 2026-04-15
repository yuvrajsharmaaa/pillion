package com.pillion

import android.app.Application
import com.pillion.di.AppContainer

class PillionApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }
}
