package com.knoux.ai

import android.app.Application

/**
 * Installs the resource resolver before the first screen is composed.
 *
 * Every customer-visible string is looked up by resource id, and the app owns its
 * own language switch, so both resource sets must exist before `Copy.text` runs.
 */
class KnouxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KnouxStrings.install(this)
    }
}
