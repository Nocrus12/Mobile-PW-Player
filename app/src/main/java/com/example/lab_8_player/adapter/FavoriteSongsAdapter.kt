package com.example.lab_8_player.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.R
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.PlaybackService

class FavoriteSongsAdapter(private val context: Context) :
    RecyclerView.Adapter<FavoriteSongsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songTitle: TextView = view.findViewById(R.id.favSongTitle)
        val songArtist: TextView = view.findViewById(R.id.favSongArtist)
    }

    private val differCallback = object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = differ.currentList[position]
        holder.songTitle.text = song.name
        holder.songArtist.text = song.artist

        holder.itemView.setOnClickListener {
            val fullList = ArrayList(differ.currentList)
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = "ACTION_BEGIN"
                putParcelableArrayListExtra("EXTRA_SONG_LIST", fullList)
                putExtra("EXTRA_PLAY_INDEX", holder.adapterPosition)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun getItemCount(): Int = differ.currentList.size
}
