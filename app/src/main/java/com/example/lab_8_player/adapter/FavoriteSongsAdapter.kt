package com.example.lab_8_player.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.R
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.PlaybackService

class FavoriteSongsAdapter(private val context: Context, val songList: List<Song>) :
    RecyclerView.Adapter<FavoriteSongsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songTitle: TextView = view.findViewById(R.id.favSongTitle)
        val songArtist: TextView = view.findViewById(R.id.favSongArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songList[position]
        holder.songTitle.text = song.name
        holder.songArtist.text = song.artist

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PlaybackService::class.java).apply {
            action = "ACTION_PLAY"
            putExtra("SONG_URI", song.uri)
        }
        ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun getItemCount(): Int = songList.size
}
