package org.hnau.ktiot.client.android

import android.app.Application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter

class App: Application() {

    init {
        Logger.setLogWriters(platformLogWriter())
    }
}