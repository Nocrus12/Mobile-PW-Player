package com.example.lab_8_player.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.R
import com.example.lab_8_player.db.model.Song
import com.example.lab_8_player.PlaybackService

class AllSongsAdapter(
    private val context: Context,
    private val onFavoriteClick: (Song) -> Unit,
    private val onAddToPlaylistClick: (Song) -> Unit
) : RecyclerView.Adapter<AllSongsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songTitle: TextView = view.findViewById(R.id.textTrackTitle)
        val songArtist: TextView = view.findViewById(R.id.textTrackArtist)
        val songDuration: TextView = view.findViewById(R.id.textTrackDuration)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
        val btnAddToPlaylist: ImageButton = view.findViewById(R.id.btnAddToPlaylist)
    }

    private val differCallback = object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.isFavorite == newItem.isFavorite &&
                    oldItem.name == newItem.name &&
                    oldItem.artist == newItem.artist
        }

    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = differ.currentList[position]

        holder.songTitle.text = song.name
        holder.songArtist.text = song.artist
        holder.songDuration.text = song.duration.toString()

        // Set icon based on favorite status
        if (song.isFavorite) {
            holder.btnFavorite.setImageResource(R.drawable.baseline_favorite_24)
        } else {
            holder.btnFavorite.setImageResource(R.drawable.baseline_favorite_border_24)
        }

        // Clicking on song -> Play
        holder.itemView.setOnClickListener {
            val fullList = ArrayList(differ.currentList)
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = "ACTION_BEGIN"
                putParcelableArrayListExtra("EXTRA_SONG_LIST", fullList)
                putExtra("EXTRA_PLAY_INDEX", holder.adapterPosition)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        // Clicking on heart button -> Toggle favorite
        holder.btnFavorite.setOnClickListener {
            onFavoriteClick(song)
        }

        // Clicking on add to playlist button
        holder.btnAddToPlaylist.setOnClickListener {
            onAddToPlaylistClick(song)
        }
    }

    override fun getItemCount(): Int = differ.currentList.size
}
