package com.mashedpotato.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private TextView playingSongTV, currentTimeTV, totalTimeTV;
    private SeekBar seekBarSB;
    private Button playB, previousB, nextB, repeatB, shuffleB;

    private ArrayList<Song> songList;
    private Song song;
    MediaPlayer mediaPlayer = MediaPlayerService.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_player);

        playingSongTV = findViewById(R.id.idTVSongPlaying);
        currentTimeTV = findViewById(R.id.idTVCurrentTime);
        totalTimeTV = findViewById(R.id.idTVTotalTime);
        seekBarSB = findViewById(R.id.idSBBar);
        playB = findViewById(R.id.idBPlay);
        previousB = findViewById(R.id.idBPrevious);
        nextB = findViewById(R.id.idBNext);
        repeatB = findViewById(R.id.idBRepeat);
        shuffleB = findViewById(R.id.idBShuffle);

        playingSongTV.setSelected(true);

        songList = (ArrayList<Song>) getIntent().getSerializableExtra("List");

        setMusic();

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBarSB.setProgress(mediaPlayer.getCurrentPosition());
                    currentTimeTV.setText(convertTime(mediaPlayer.getCurrentPosition() + ""));

                    if (mediaPlayer.isPlaying()) {
                        playB.setBackground(getDrawable(R.drawable.ic_baseline_pause_24));
                    } else {
                        playB.setBackground(getDrawable(R.drawable.ic_baseline_play_24));
                    }
                }
                new Handler().postDelayed(this, 100);
            }
        });

        seekBarSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setMusic() {
        song = songList.get(MediaPlayerService.songIndex);

        playingSongTV.setText(song.getTitle());
        totalTimeTV.setText(convertTime(song.getDuration()));

        playB.setOnClickListener(v -> pauseSong());
        nextB.setOnClickListener(v -> playNextSong());
        previousB.setOnClickListener(v -> playPreviousSong());

        playSong();
    }

    private void playSong() {
        mediaPlayer.reset();
        try {
            mediaPlayer.prepare();
            mediaPlayer.start();
            seekBarSB.setProgress(0);
            seekBarSB.setMax(mediaPlayer.getDuration());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pauseSong() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        } else {
            mediaPlayer.start();
        }
    }

    private void playNextSong() {
        if (MediaPlayerService.songIndex == songList.size() - 1) {
           MediaPlayerService.songIndex = -1;
        }

        MediaPlayerService.songIndex++;
        mediaPlayer.reset();
        setMusic();
    }

    private void playPreviousSong() {
        if (MediaPlayerService.songIndex == 0) {
            MediaPlayerService.songIndex = songList.size();
        }

        MediaPlayerService.songIndex--;
        mediaPlayer.reset();
        setMusic();
    }

    public String convertTime(String duration) {
        long time = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
    }
}