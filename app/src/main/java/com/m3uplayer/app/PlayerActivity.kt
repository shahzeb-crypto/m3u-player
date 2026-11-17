package com.m3uplayer.app

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

class PlayerActivity : Activity() {

    private lateinit var libVLC: LibVLC
    private lateinit var player: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var label: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val items = PlaylistHolder.items
    private var index = 0
    private var useHw = false
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        index = intent.getIntExtra("index", 0)
        useHw = intent.getBooleanExtra("hw", false)

        videoLayout = VLCVideoLayout(this)
        label = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#99000000"))
            textSize = 18f
            setPadding(32, 16, 32, 16)
            visibility = View.GONE
        }

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        root.addView(
            videoLayout,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        root.addView(
            label,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        )
        setContentView(root)

        libVLC = LibVLC(this, arrayListOf<String>())
        player = MediaPlayer(libVLC)
        player.setEventListener { e ->
            when (e.type) {
                MediaPlayer.Event.EndReached -> next()
                MediaPlayer.Event.EncounteredError -> showLabel("Can't play this item. Press UP for next.")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        player.attachViews(videoLayout, null, false, false)
        player.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
        if (!started) {
            started = true
            if (items.isEmpty()) finish() else playAt(index)
        } else {
            player.play()
        }
    }

    override fun onStop() {
        super.onStop()
        player.pause()
        player.detachViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player.release()
        libVLC.release()
    }

    private fun playAt(i: Int) {
        if (items.isEmpty()) return
        index = ((i % items.size) + items.size) % items.size
        val item = items[index]
        val media = Media(libVLC, toUri(item.uri))
        media.setHWDecoderEnabled(useHw, false)
        player.media = media
        media.release()
        player.play()
        showLabel("${index + 1}/${items.size}  ${item.title}")
    }

    private fun next() = playAt(index + 1)
    private fun prev() = playAt(index - 1)

    private fun toUri(s: String): Uri =
        if (s.startsWith("/")) Uri.fromFile(File(s)) else Uri.parse(s)

    private fun seek(ms: Long) {
        val len = player.length
        var t = player.time + ms
        if (t < 0) t = 0
        if (len > 0 && t > len) t = len
        player.time = t
    }

    private fun showLabel(text: String) {
        label.text = text
        label.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ label.visibility = View.GONE }, 3500)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { seek(10_000); return true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> { seek(-10_000); return true }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> { next(); return true }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { prev(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}
