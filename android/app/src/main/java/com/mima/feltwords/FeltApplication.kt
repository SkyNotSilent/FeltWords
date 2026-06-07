package com.mima.feltwords

import android.app.Application
import com.mima.feltwords.data.ServiceLocator

class FeltApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
