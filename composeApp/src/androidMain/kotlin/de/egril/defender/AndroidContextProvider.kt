package de.egril.defender

import android.content.Context

/**
 * Provides access to the Android application context
 * Must be initialized in MainActivity before any FileStorage operations
 */
object AndroidContextProvider {
    private var applicationContext: Context? = null
    
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    fun getContext(): Context {
        return applicationContext 
            ?: throw IllegalStateException("AndroidContextProvider not initialized. Call initialize() in MainActivity.onCreate()")
    }
}
