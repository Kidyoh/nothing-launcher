package com.kidyoh.nothinglauncher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small persisted settings store, exposed as flows so Compose recomposes on change. */
object Prefs {
    private lateinit var sp: SharedPreferences

    private val _favorites = MutableStateFlow<List<String>>(emptyList())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _systemWallpaper = MutableStateFlow(false)
    val systemWallpaper: StateFlow<Boolean> = _systemWallpaper.asStateFlow()

    private val _monoIcons = MutableStateFlow(true)
    val monoIcons: StateFlow<Boolean> = _monoIcons.asStateFlow()

    private val _shadeEnabled = MutableStateFlow(true)
    val shadeEnabled: StateFlow<Boolean> = _shadeEnabled.asStateFlow()

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.applicationContext.getSharedPreferences("nothing", Context.MODE_PRIVATE)
        _favorites.value = sp.getString("favorites", null)?.split('|')?.filter { it.isNotEmpty() } ?: emptyList()
        _systemWallpaper.value = sp.getBoolean("system_wallpaper", false)
        _monoIcons.value = sp.getBoolean("mono_icons", true)
        _shadeEnabled.value = sp.getBoolean("shade_enabled", true)
    }

    val favoritesInitialized: Boolean get() = sp.contains("favorites")

    var setupDone: Boolean
        get() = sp.getBoolean("setup_done", false)
        set(v) = sp.edit().putBoolean("setup_done", v).apply()

    fun setFavorites(keys: List<String>) {
        _favorites.value = keys
        sp.edit().putString("favorites", keys.joinToString("|")).apply()
    }

    /** Puts [key] into [slot], growing the list with blanks as needed. Blank slots are "". */
    fun setFavorite(slot: Int, key: String) {
        val list = _favorites.value.toMutableList()
        while (list.size <= slot) list.add("")
        list[slot] = key
        setFavorites(list)
    }

    /** Adds [key] to the first free home slot. Returns false when the home screen is full. */
    fun addFavorite(key: String, slots: Int = AppRepository.HOME_SLOTS): Boolean {
        val list = _favorites.value.toMutableList()
        if (key in list) return true
        val blank = list.indexOf("")
        when {
            blank in 0 until slots -> list[blank] = key
            list.size < slots -> list.add(key)
            else -> return false
        }
        setFavorites(list)
        return true
    }

    fun setSystemWallpaper(v: Boolean) {
        _systemWallpaper.value = v; sp.edit().putBoolean("system_wallpaper", v).apply()
    }

    fun setMonoIcons(v: Boolean) {
        _monoIcons.value = v; sp.edit().putBoolean("mono_icons", v).apply()
    }

    fun setShadeEnabled(v: Boolean) {
        _shadeEnabled.value = v; sp.edit().putBoolean("shade_enabled", v).apply()
    }
}
