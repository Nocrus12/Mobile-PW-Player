package com.example.lab_8_player.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.lab_8_player.PlaybackService
import com.example.lab_8_player.R
import com.example.lab_8_player.databinding.FragmentMiniPlayerBinding
import com.example.lab_8_player.db.AppDatabase
import com.example.lab_8_player.repository.SongRepository
import com.example.lab_8_player.viewmodel.SongViewModel
import com.example.lab_8_player.viewmodel.SongViewModelFactory
import kotlinx.coroutines.launch
import kotlin.getValue

class MiniPlayerFragment : Fragment() {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var receiver: BroadcastReceiver
    private var isPlaying = false
    private var currentSongId = -1L
    private var currentDurationMs: Int = 0

    private val db by lazy { AppDatabase.getInstance(requireContext().applicationContext) }

    // Setup viewmodel via factory to fetch song
    private val songViewModel: SongViewModel by viewModels {
        SongViewModelFactory(requireActivity().application, SongRepository(db.songDao()))
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMiniPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.miniPlayer.visibility = View.GONE

        setupReceiver()
    }


    private fun setupReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                currentSongId = intent.getLongExtra("songId", -1L)
                isPlaying     = intent.getBooleanExtra("isPlaying", false)
                val pos       = intent.getIntExtra("position", 0)

                // 1) Update play/pause button
                binding.btnPlayPause.setImageResource(
                    if (isPlaying) R.drawable.baseline_pause_24
                    else R.drawable.baseline_play_arrow_24
                )

                // 2) Update seekbar
                binding.miniSeekBar.apply {
                    // Don’t know duration yet; Set max after fetching song
                    progress = pos
                }

                // 3) Fetch the full Song from the DB
                viewLifecycleOwner.lifecycleScope.launch {
                    val song = songViewModel.getSongById(currentSongId)
                    // update UI on main thread
                    binding.apply {
                        miniPlayer.visibility = View.VISIBLE
                        miniTitle.text   = song.name
                        miniArtist.text  = song.artist
                        currentDurationMs = song.duration
                        // configure seekBar max
                        miniSeekBar.max = currentDurationMs
                        miniSeekBar.progress = pos
                        // favorite button
                        btnFavorite.apply {
                            isSelected = song.isFavorite
                            setImageResource(
                                if (song.isFavorite) R.drawable.baseline_favorite_24
                                else R.drawable.baseline_favorite_border_24
                            )
                        }
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(receiver, IntentFilter("PLAYBACK_STATE_CHANGED"))

        // Controls
        binding.btnPlayPause.setOnClickListener {
            val action = if (isPlaying) "ACTION_PAUSE" else "ACTION_RESUME"
            val intent = Intent(context, PlaybackService::class.java).apply {
                this.action = action
            }
            ContextCompat.startForegroundService(requireContext(), intent)
        }

        binding.btnPrev.setOnClickListener {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = "ACTION_PREV"
            }
            ContextCompat.startForegroundService(requireContext(), intent)
        }

        binding.btnNext.setOnClickListener {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = "ACTION_NEXT"
            }
            ContextCompat.startForegroundService(requireContext(), intent)
        }

        binding.btnFavorite.setOnClickListener {
            val newFav = !binding.btnFavorite.isSelected
            lifecycleScope.launch {
                songViewModel.toggleFavSong(currentSongId, newFav)
            }
            binding.btnFavorite.apply {
                isSelected = newFav
                setImageResource(
                    if (newFav) R.drawable.baseline_favorite_24
                    else R.drawable.baseline_favorite_border_24
                )
            }
        }


        binding.miniSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && currentDurationMs > 0) {
                    val targetMs = (progress / 1000.0 * currentDurationMs).toInt()
                    val intent = Intent(context, PlaybackService::class.java).apply {
                        action = "ACTION_SEEK"
                        putExtra("EXTRA_SEEK_TO", progress)
                    }
                    ContextCompat.startForegroundService(requireContext(), intent)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

    }


    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        _binding = null
        super.onDestroy()
    }
}
