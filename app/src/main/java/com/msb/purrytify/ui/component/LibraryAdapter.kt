package com.msb.purrytify.ui.component

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.target.ImageViewTarget
import com.msb.purrytify.R
import com.msb.purrytify.data.local.entity.Song
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import java.util.Locale

class LibraryAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit,
): RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>(){

    inner class LibraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val title: TextView = itemView.findViewById<TextView>(R.id.song_title)
        private val artist: TextView = itemView.findViewById<TextView>(R.id.song_artist)
        private val duration: TextView = itemView.findViewById<TextView>(R.id.song_duration)
        private val artwork: ImageView = itemView.findViewById<ImageView>(R.id.song_artwork)

        fun bind(song: Song){
            title.text = song.title
            artist.text = song.artistName

            val minutes = TimeUnit.MILLISECONDS.toMinutes(song.duration)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(song.duration) - TimeUnit.MINUTES.toSeconds(minutes)
            duration.text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)

            val context = itemView.context
            val artworkPath = song.artworkPath

            if (artworkPath.isNotEmpty()) {
                val request = when {
                    // Ini klo artwork online songs
                    artworkPath.startsWith("http") -> {
                        ImageRequest.Builder(context)
                            .data(artworkPath)
                            .target(ImageViewTarget(artwork))
                            .crossfade(true)
                            .build()
                    }
                    // Ini klo artwork lagu biasa
                    artworkPath.startsWith("content:") -> {
                        ImageRequest.Builder(context)
                            .data(artworkPath.toUri())
                            .target(ImageViewTarget(artwork))
                            .crossfade(true)
                            .build()
                    }
                    // Ini klo artwork lagu download
                    artworkPath.startsWith(context.filesDir.absolutePath) -> {
                        ImageRequest.Builder(context)
                            .data(File(artworkPath))
                            .target(ImageViewTarget(artwork))
                            .crossfade(true)
                            .build()
                    }
                    // Ini artwork default
                    else -> {
                        ImageRequest.Builder(context)
                            .data(File(artworkPath))
                            .target(ImageViewTarget(artwork))
                            .crossfade(true)
                            .build()
                    }
                }
                
                ImageLoader(context).enqueue(request)
            } else {
                artwork.setImageResource(R.drawable.image)
            }

            itemView.setOnClickListener {
                onSongClick(song)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_library, parent, false)
        return LibraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int){
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int{
        return songs.size
    }
}