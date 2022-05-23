package com.mashedpotato.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

interface ItemClickListener {
    void onClick(View view, int position,boolean isLongClick);
}

class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener
{
    public LinearLayout playerLL;
    public TextView songTV, artistTV;

    private ItemClickListener itemClickListener;

    public RecyclerViewHolder(View itemView) {
        super(itemView);
        playerLL = (LinearLayout)itemView.findViewById(R.id.idLLPlayer);
        songTV = itemView.findViewById(R.id.idTVSong);
        artistTV = itemView.findViewById(R.id.idTVArtist);
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
        View view = inflater.inflate(R.layout.song_list, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.songTV.setText(song.getTitle());
        holder.artistTV.setText(song.getArtist());

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
