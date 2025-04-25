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

class AllSongsAdapter(private val context: Context, val songList: List<Song>) :
    RecyclerView.Adapter<AllSongsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songTitle: TextView = view.findViewById(R.id.textTrackTitle)
        val songArtist: TextView = view.findViewById(R.id.textTrackArtist)
        val songDuration: TextView = view.findViewById(R.id.textTrackDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songList[position]
        holder.songTitle.text = song.name
        holder.songArtist.text = song.artist
        holder.songDuration.text = song.duration.toString()

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = "ACTION_START"
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }


    override fun getItemCount(): Int = songList.size
}
