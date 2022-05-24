package com.mashedpotato.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private TextView playingSongTV, currentTimeTV, totalTimeTV;
    private SeekBar seekBarSB;
    private Button playB, previousB, nextB, repeatB, shuffleB;

    // 0 is no loop
    // 1 is loop all songs
    // 2 is loop one only
    private int repeatMode = 0;
    public static boolean shuffle = false;

    private ArrayList<Song> songList;
    private ArrayList<Song> songListOrigin;
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
        songListOrigin = (ArrayList<Song>) songList.clone();

        setMusic();

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBarSB.setProgress(mediaPlayer.getCurrentPosition());
                    SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss");
                    currentTimeTV.setText(timeFormat.format(mediaPlayer.getCurrentPosition()));

                    if (mediaPlayer.isPlaying()) {
                        playB.setBackgroundResource(R.drawable.ic_baseline_pause_24);
                    } else {
                        playB.setBackgroundResource(R.drawable.ic_baseline_play_24);

                        // About repeat mode
                        // 0 is no loop
                        // 1 is loop all
                        // 2 is loop one only
                        if (repeatMode == 1) {
                            playNextSong();
                        } else if (repeatMode == 2) {
                            mediaPlayer.start();
                            mediaPlayer.setLooping(true);
                        }
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
        shuffleB.setOnClickListener(v -> shuffleSong());
        repeatB.setOnClickListener(v -> repeatSong());

        playSong();
    }

    private void playSong() {
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(song.getData());
            mediaPlayer.prepare();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();

        seekBarSB.setProgress(0);
        seekBarSB.setMax(mediaPlayer.getDuration());
    }

    private void pauseSong() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    private void repeatSong() {
        if (repeatMode == 0) {
            repeatB.setBackgroundResource(R.drawable.ic_baseline_loop_24);  // to loop all songs
            repeatMode = 1;
            System.out.println(repeatMode);
        } else if (repeatMode == 1) {
            repeatB.setBackgroundResource(R.drawable.ic_baseline_repeat_one_24);    // to loop one song
            repeatMode = 2;
            System.out.println(repeatMode);
        } else if (repeatMode == 2) {
            repeatB.setBackgroundResource(R.drawable.ic_baseline_repeat_24);    // no loop
            repeatMode = 0;
            System.out.println(repeatMode);
        }
    }

    private void shuffleSong() {
        Context context = this;
        if (shuffle) {
            songList = songListOrigin;
            shuffleB.setBackgroundResource(R.drawable.ic_baseline_shuffle_24);
            Toast.makeText(context, "Unshuffle", Toast.LENGTH_SHORT).show();
            shuffle = false;
        } else {
            Collections.shuffle(songList);
            shuffleB.setBackgroundResource(R.drawable.ic_baseline_shuffle_24);
            Toast.makeText(context, "Shuffle", Toast.LENGTH_SHORT).show();
            shuffle = true;
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