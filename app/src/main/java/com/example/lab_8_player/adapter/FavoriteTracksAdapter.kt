package com.example.lab_8_player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lab_8_player.databinding.ItemFavoriteSongBinding
import com.example.lab_8_player.db.model.Song

class FavoriteTracksAdapter(
    private val tracks: List<Song>,
    private val onClick: (Song) -> Unit
) : RecyclerView.Adapter<FavoriteTracksAdapter.FavTrackViewHolder>() {

    inner class FavTrackViewHolder(private val binding: ItemFavoriteSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.track = song
            binding.root.setOnClickListener { onClick(song) }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavTrackViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFavoriteSongBinding.inflate(inflater, parent, false)
        return FavTrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavTrackViewHolder, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size
}
