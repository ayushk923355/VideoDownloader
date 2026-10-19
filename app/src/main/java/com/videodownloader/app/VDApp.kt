package com.videodownloader.app

import android.app.Application
import com.videodownloader.app.engine.Engine

class VDApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
    }
}
