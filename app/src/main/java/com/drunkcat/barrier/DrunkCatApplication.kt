package com.drunkcat.barrier

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

/**
 * Application class: initialise osmdroid configuration.
 */
class DrunkCatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialise osmdroid configuration from shared preferences
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        // Set userAgent to application package name
        Configuration.getInstance().userAgentValue = packageName
    }
}
