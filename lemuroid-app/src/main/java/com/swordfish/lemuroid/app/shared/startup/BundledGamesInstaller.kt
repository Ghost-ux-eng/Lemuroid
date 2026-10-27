package com.swordfish.lemuroid.app.shared.startup

import android.content.Context
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import timber.log.Timber
import java.io.File
import kotlin.concurrent.thread

object BundledGamesInstaller {
    private const val ASSETS_FOLDER = "bundled-games"
    private const val PREFS_NAME = "bundled_games"
    private const val PREFS_KEY_PREFIX = "installed_"

    private val ROM_EXTENSIONS =
        setOf("gb", "gbc", "gba", "nes", "sfc", "smc", "gen", "sms", "gg", "pce", "a26", "a78", "lnx", "ngp", "ngc", "zip")

    fun installInBackground(context: Context) {
        val appContext = context.applicationContext
        thread(name = "bundled-games-installer") {
            try {
                if (install(appContext)) {
                    LibraryIndexScheduler.scheduleLibrarySync(appContext)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to install bundled games")
            }
        }
    }

    private fun install(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val assetNames = context.assets.list(ASSETS_FOLDER) ?: return false
        val romsDirectory = DirectoriesManager(context).getInternalRomsDirectory()
        var installedAny = false

        assetNames
            .filter { it.substringAfterLast('.', "").lowercase() in ROM_EXTENSIONS }
            .forEach { name ->
                val key = PREFS_KEY_PREFIX + name
                if (prefs.getBoolean(key, false)) return@forEach

                val target = File(romsDirectory, name)
                if (!target.exists()) {
                    context.assets.open("$ASSETS_FOLDER/$name").use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                prefs.edit().putBoolean(key, true).apply()
                installedAny = true
            }

        return installedAny
    }
}
