package tv.polya.app

import android.content.Context

class LauncherPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("polya_launcher", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()
    fun hidden(): Set<String> = preferences.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()

    fun setFavorite(packageName: String, favorite: Boolean) = updateSet(KEY_FAVORITES, packageName, favorite)
    fun setHidden(packageName: String, hidden: Boolean) = updateSet(KEY_HIDDEN, packageName, hidden)

    private fun updateSet(key: String, packageName: String, enabled: Boolean) {
        val values = preferences.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        if (enabled) values.add(packageName) else values.remove(packageName)
        preferences.edit().putStringSet(key, values).apply()
    }

    private companion object {
        const val KEY_FAVORITES = "favorite_apps"
        const val KEY_HIDDEN = "hidden_apps"
    }
}
