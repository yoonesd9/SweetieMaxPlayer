// MainActivity.kt
package com.example.sweetiemaxplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.media.MediaPlayer
import android.widget.ImageButton
import android.widget.TextView
import android.widget.SeekBar
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MediaAdapter
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playPauseBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var prevBtn: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var timeView: TextView
    private lateinit var titleView: TextView
    
    private var mediaList = mutableListOf<File>()
    private var currentIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        setupMediaPlayer()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        nextBtn = findViewById(R.id.nextBtn)
        prevBtn = findViewById(R.id.prevBtn)
        seekBar = findViewById(R.id.seekBar)
        timeView = findViewById(R.id.timeView)
        titleView = findViewById(R.id.titleView)

        adapter = MediaAdapter(mediaList) { file, position ->
            currentIndex = position
            playMedia(file)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        playPauseBtn.setOnClickListener { togglePlayPause() }
        nextBtn.setOnClickListener { playNext() }
        prevBtn.setOnClickListener { playPrev() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
        } else {
            loadMedia()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMedia()
        }
    }

    private fun loadMedia() {
        val musicDir = File("/storage/emulated/0/Music")
        val videoDir = File("/storage/emulated/0/Movies")

        musicDir.listFiles()?.filter { it.extension in listOf("mp3", "wav", "flac") }?.forEach {
            mediaList.add(it)
        }
        videoDir.listFiles()?.filter { it.extension in listOf("mp4", "mkv", "avi") }?.forEach {
            mediaList.add(it)
        }

        adapter.notifyDataSetChanged()
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnCompletionListener {
            playNext()
        }
    }

    private fun playMedia(file: File) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
            titleView.text = file.nameWithoutExtension
            seekBar.max = mediaPlayer.duration
            updateSeekBar()
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
        } else {
            mediaPlayer.start()
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            updateSeekBar()
        }
    }

    private fun playNext() {
        if (currentIndex < mediaList.size - 1) {
            currentIndex++
            playMedia(mediaList[currentIndex])
        }
    }

    private fun playPrev() {
        if (currentIndex > 0) {
            currentIndex--
            playMedia(mediaList[currentIndex])
        }
    }

    private fun updateSeekBar() {
        if (mediaPlayer.isPlaying) {
            seekBar.progress = mediaPlayer.currentPosition
            timeView.text = "${formatTime(mediaPlayer.currentPosition)} / ${formatTime(mediaPlayer.duration)}"
            handler.postDelayed({ updateSeekBar() }, 1000)
        }
    }

    private fun formatTime(ms: Int): String {
        return String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(ms.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacksAndMessages(null)
    }
}
