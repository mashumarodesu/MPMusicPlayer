package com.mashedpotato.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;


class RecyclerViewHolder extends RecyclerView.ViewHolder {
    public ConstraintLayout playerCL;
    public TextView songTV, artistTV;
    public ImageView coverIV;

    public RecyclerViewHolder(View itemView) {
        super(itemView);
        playerCL = itemView.findViewById(R.id.idCLPlayer);
        songTV = itemView.findViewById(R.id.idTVSong);
        artistTV = itemView.findViewById(R.id.idTVArtist);
        coverIV = itemView.findViewById(R.id.idIVCover);
    }
}


public class Adapter extends RecyclerView.Adapter<RecyclerViewHolder> implements Filterable {
    private Context context;
    private ArrayList<Song> songList;   // origin
    private ArrayList<Song> songListFiltered;   // call ra

    public Adapter(Context context, ArrayList<Song> songList) {
        this.context = context;
        this.songList = songList;
        this.songListFiltered = songList;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.song_cardview, parent, false);
        return new RecyclerViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {

        ContentResolver contentResolver = context.getContentResolver();

        Song song = songListFiltered.get(position);
        holder.songTV.setText(song.getTitle());
        holder.artistTV.setText(song.getArtist());

//        try {
//            Bitmap cover = contentResolver.loadThumbnail(Uri.parse(song.getUriString()), new Size(500, 500), null);
//            holder.coverIV.setImageBitmap(cover);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public Filter getFilter() throws NullPointerException {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    songListFiltered = songList;
                } else {
                    ArrayList<Song> listFiltered = new ArrayList<>();
                    for (Song song : songList) {
                        // in case of misunderstanding
                        // if (name match something something)
                        if (song.getTitle().toLowerCase().contains(charString.toLowerCase()) ||
                                song.getArtist().toLowerCase().contains(charString.toLowerCase()) ||
                                song.getAlbum().toLowerCase().contains(charString.toLowerCase())) {
                            listFiltered.add(song);
                        }
                    }

                    songListFiltered = listFiltered;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = songListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                songListFiltered = (ArrayList<Song>) filterResults.values;
                notifyDataSetChanged();
            }
        };
        return filter;
    }

    @Override
    public int getItemCount() {
        try {
            return songListFiltered.size();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}
