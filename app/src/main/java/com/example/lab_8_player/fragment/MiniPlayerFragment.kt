package com.example.lab_8_player.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.lab_8_player.PlaybackService
import com.example.lab_8_player.R
import com.example.lab_8_player.databinding.FragmentMiniPlayerBinding

class MiniPlayerFragment : Fragment() {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var receiver: BroadcastReceiver
    private var isPlaying = false
    private var currentDurationMs: Int = 0

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

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val title = intent.getStringExtra("title") ?: ""
                val artist = intent.getStringExtra("artist") ?: ""
                isPlaying = intent.getBooleanExtra("isPlaying", false)
                val duration = intent.getIntExtra("duration", 0)
                val pos = intent.getIntExtra("position", 0)
                currentDurationMs = duration

                if (duration > 0) {
                    binding.miniSeekBar.max = 1000
                    binding.miniSeekBar.progress = (pos * 1000L / duration).toInt()
                }

                binding.miniPlayer.visibility = View.VISIBLE
                binding.miniTitle.text = title
                binding.miniArtist.text = artist
                binding.btnPlayPause.setImageResource(
                    if (isPlaying) R.drawable.baseline_pause_24_white else R.drawable.baseline_play_arrow_24_white
                )
            }
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(receiver, IntentFilter("PLAYBACK_STATE_CHANGED"))

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

        binding.miniSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && currentDurationMs > 0) {
                    val targetMs = (progress / 1000.0 * currentDurationMs).toInt()
                    val intent = Intent(context, PlaybackService::class.java).apply {
                        action = "ACTION_SEEK"
                        putExtra("EXTRA_SEEK_TO", targetMs)
                    }
                    ContextCompat.startForegroundService(requireContext(), intent)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

//        binding.miniPlayer.setOnTouchListener { v, ev ->
//            when (ev.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    val controls = listOf(
//                        binding.btnPrev, binding.btnPlayPause,
//                        binding.btnNext, binding.miniSeekBar
//                    )
//                    val hitAControl = controls.any { child ->
//                        val r = Rect().also { child.getHitRect(it) }
//                        r.contains(ev.x.toInt(), ev.y.toInt())
//                    }
//
//                    if (!hitAControl) {
//                        v.performClick()
//                        return@setOnTouchListener true
//                    }
//                }
//            }
//            false
//        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        _binding = null
        super.onDestroy()
    }
}
