package com.example.lab_8_player.db

import android.content.Context

class PlayerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)

    var lastPlayedUri: String?
        get() = prefs.getString("last_played_uri", null)
        set(value) = prefs.edit().putString("last_played_uri", value).apply()

    var lastPosition: Int
        get() = prefs.getInt("last_position", 0)
        set(value) = prefs.edit().putInt("last_position", value).apply()
}

