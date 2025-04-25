package com.example.lab_8_player.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.R
import com.example.lab_8_player.db.model.Playlist

class PlaylistsAdapter(private val context: Context, val playlists: List<Playlist>) :
    RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playlistCover: ImageView = view.findViewById(R.id.playlistCover)
        val playlistTitle: TextView = view.findViewById(R.id.playlistTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.playlistTitle.text = playlist.name

        // Placeholder image used, you can load real ones if needed
        holder.playlistCover.setImageResource(R.drawable.ic_playlist_placeholder)
    }

    override fun getItemCount(): Int = playlists.size
}
