package de.psaimusic.dienstplaner

import android.content.Context
import de.psaimusic.dienstplaner.data.AppStore

object AppGlobals {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun store(): AppStore? = appContext?.let { AppStore(it) }
}
