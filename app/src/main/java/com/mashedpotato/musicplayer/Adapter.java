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


public class Adapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private Context context;
    private ArrayList<Song> songList;

    public Adapter(Context context, ArrayList<Song> songList) {
        this.context = context;
        this.songList = songList;
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

        Song song = songList.get(position);
        holder.songTV.setText(song.getTitle());
        holder.artistTV.setText(song.getArtist());

        try {
            Bitmap cover = contentResolver.loadThumbnail(Uri.parse(song.getUriString()), new Size(500, 500), null);
            holder.coverIV.setImageBitmap(cover);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        try {
            return songList.size();
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
