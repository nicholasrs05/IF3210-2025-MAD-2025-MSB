package com.msb.purrytify.ui.component

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.msb.purrytify.R

data class Song(
    val title: String,
    val artist: String
)

class LibraryAdapter(private val songs: List<Song>):
    RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>(){

    inner class LibraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val title = itemView.findViewById<TextView>(R.id.song_title)
        val artist = itemView.findViewById<TextView>(R.id.song_artist)

        fun bind(song: Song){
            title.text = song.title
            artist.text = song.artist

            itemView.setOnClickListener {
                // Handle item click
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