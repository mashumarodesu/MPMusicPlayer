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

interface ItemClickListener {
    void onClick(View view, int position,boolean isLongClick);
}

class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener
{
    public ConstraintLayout playerCL;
    public TextView songTV, artistTV;
    public ImageView coverIV;

    private ItemClickListener itemClickListener;

    public RecyclerViewHolder(View itemView) {
        super(itemView);
        playerCL = itemView.findViewById(R.id.idCLPlayer);
        songTV = itemView.findViewById(R.id.idTVSong);
        artistTV = itemView.findViewById(R.id.idTVArtist);
        coverIV = itemView.findViewById(R.id.idIVCover);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void setItemClickListener(ItemClickListener itemClickListener)
    {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View v) {
        itemClickListener.onClick(v,getAdapterPosition(),false);
    }

    @Override
    public boolean onLongClick(View v) {
        itemClickListener.onClick(v,getAdapterPosition(),true);
        return true;
    }
}


public class Adapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private Context context;
    private ArrayList<Song> songList = new ArrayList<>();

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

        holder.setItemClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position, boolean isLongClick) {
                if(isLongClick) {
                    Toast.makeText(context, "Long Click: " + songList.get(position), Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(context, " "+songList.get(position), Toast.LENGTH_SHORT).show();
                    MediaPlayerService.getInstance().reset();
                    MediaPlayerService.songIndex = position;
                    Intent intent = new Intent(context, PlayerActivity.class);
                    intent.putExtra("List", songList);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });
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
}
