package com.m3uplayer.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var list: ListView
    private lateinit var status: TextView
    private lateinit var hwBox: CheckBox

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO),
                100
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val b = insets.getInsets(WindowInsets.Type.systemBars())
                    v.setPadding(dp(20) + b.left, dp(20) + b.top, dp(20) + b.right, dp(20) + b.bottom)
                }
                insets
            }
        }

        val title = TextView(this).apply {
            text = "M3U Player"
            textSize = 24f
            setTextColor(Color.WHITE)
        }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnM3u = Button(this).apply {
            text = "Open .m3u file"
            setOnClickListener { pickM3u() }
        }
        val btnFiles = Button(this).apply {
            text = "Pick video files"
            setOnClickListener { pickVideos() }
        }
        row.addView(btnM3u, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(btnFiles, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        hwBox = CheckBox(this).apply {
            text = "Use hardware decoding (leave OFF if video doesn't show)"
            setTextColor(Color.LTGRAY)
            isChecked = false
        }

        status = TextView(this).apply {
            text = "Open a .m3u file, or pick video files, to start."
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(8))
        }

        list = ListView(this)
        list.setOnItemClickListener { _, _, position, _ -> play(position) }

        root.addView(title)
        root.addView(row)
        root.addView(hwBox)
        root.addView(status)
        root.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        // Opened from a file manager ("Open with M3U Player")
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            loadM3u(intent.data!!)
        }
    }

    private fun pickM3u() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startPicker(i, 1)
    }

    private fun pickVideos() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "audio/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startPicker(i, 2)
    }

    @Suppress("DEPRECATION")
    private fun startPicker(i: Intent, code: Int) {
        try {
            startActivityForResult(i, code)
        } catch (e: Exception) {
            Toast.makeText(this, "No file picker found on this device.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {
            1 -> data.data?.let { loadM3u(it) }
            2 -> {
                val uris = mutableListOf<Uri>()
                data.clipData?.let { c -> for (k in 0 until c.itemCount) uris += c.getItemAt(k).uri }
                data.data?.let { uris += it }
                val items = uris.map { u ->
                    keepAccess(u)
                    M3uItem(displayName(u), u.toString())
                }.sortedBy { it.title.lowercase() }
                show(items, 0)
            }
        }
    }

    private fun loadM3u(uri: Uri) {
        keepAccess(uri)
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IllegalStateException("Can't read file")
            val result = M3uParser.parse(text)
            show(result.items, result.skipped)
        } catch (e: Exception) {
            status.text = "Couldn't read the playlist: ${e.message}"
        }
    }

    private fun show(items: List<M3uItem>, skipped: Int) {
        PlaylistHolder.items = items
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.map { it.title })
        var msg = "${items.size} item(s) loaded."
        if (skipped > 0) {
            msg += " $skipped entries skipped (Windows or relative paths like C:\\... can't be opened " +
                "on Android). Use \"Pick video files\" for local videos."
        }
        status.text = msg
        if (items.isNotEmpty()) list.requestFocus()
    }

    private fun play(index: Int) {
        val i = Intent(this, PlayerActivity::class.java)
            .putExtra("index", index)
            .putExtra("hw", hwBox.isChecked)
        startActivity(i)
    }

    private fun keepAccess(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
    }

    private fun displayName(uri: Uri): String {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: uri.toString()
    }
}
