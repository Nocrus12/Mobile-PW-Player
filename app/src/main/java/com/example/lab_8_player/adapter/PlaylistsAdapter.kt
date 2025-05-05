package com.example.lab_8_player.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.R
import com.example.lab_8_player.db.model.Playlist

class PlaylistsAdapter(
    private val context: Context,
    private val onAddPlaylistClicked: () -> Unit, // <-- callback for Add button
    private val onPlaylistClicked: (playlistId: Long) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_PLAYLIST = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
    }

    class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playlistCover: ImageView = view.findViewById(R.id.playlistCover)
        val playlistTitle: TextView = view.findViewById(R.id.playlistTitle)
    }

    class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private val differCallback = object : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id && oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun getItemViewType(position: Int): Int {
        return if (position < differ.currentList.size) VIEW_TYPE_PLAYLIST else VIEW_TYPE_ADD_BUTTON
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_PLAYLIST) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_playlist, parent, false)
            PlaylistViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_create_playlist, parent, false)
            AddButtonViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlaylistViewHolder) {
            val playlist = differ.currentList[position]
            holder.playlistTitle.text = playlist.name
            holder.playlistCover.setImageResource(R.drawable.baseline_playlist_play_24)

            holder.itemView.setOnClickListener {
                onPlaylistClicked(playlist.id)
            }
        } else if (holder is AddButtonViewHolder) {
            holder.itemView.setOnClickListener {
                onAddPlaylistClicked()
            }
        }
    }

    override fun getItemCount(): Int = differ.currentList.size + 1 // +1 for Add button
}
