package com.mashedpotato.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Size;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private TextView playingSongTV, artistTV, currentTimeTV, totalTimeTV;
    private ImageView coverIV;
    private SeekBar seekBarSB;
    private ImageButton playB, previousB, nextB, repeatB, shuffleB;

    // 0 is no loop
    // 1 is loop all songs
    // 2 is loop one only
    private int repeatMode = 0;
    public static boolean shuffle = false;

    private ArrayList<Song> songList;
    private ArrayList<Song> songListOrigin;
    private Song song;
    MediaPlayer mediaPlayer = MediaPlayerService.getMediaPlayer();

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playingSongTV = findViewById(R.id.idTVPlayingSong);
        artistTV = findViewById(R.id.idTVArtist);
        coverIV = findViewById(R.id.idCover);
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
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBarSB.setProgress(mediaPlayer.getCurrentPosition());
                    SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss");
                    currentTimeTV.setText(timeFormat.format(mediaPlayer.getCurrentPosition()));

                    if (mediaPlayer.isPlaying()) {
                        playB.setImageResource(R.drawable.ic_baseline_pause_24);
                    } else {
                        playB.setImageResource(R.drawable.ic_baseline_play_24);

                        // About repeat mode
                        // 0 is no loop
                        // 1 is loop all
                        // 2 is loop one only
                        if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration()) {
                            playNextSong();
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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void setMusic() {

        ContentResolver contentResolver = getContentResolver();

        song = songList.get(MediaPlayerService.songIndex);

        playingSongTV.setText(song.getTitle());
        artistTV.setText(song.getArtist());
        totalTimeTV.setText(convertTime(song.getDuration()));

//        try {
//            Bitmap cover = contentResolver.loadThumbnail(Uri.parse(song.getUriString()), new Size(500, 500), null);
//            coverIV.setImageBitmap(cover);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        playB.setOnClickListener(v -> pauseSong());
        nextB.setOnClickListener(view -> forceNext());
        previousB.setOnClickListener(v -> forcePrevious());
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
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
            repeatB.setColorFilter(Color.argb(255, 30, 215, 96));
            repeatMode = 1;
        } else if (repeatMode == 1) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_one_24);    // to loop one song
            repeatB.setColorFilter(Color.argb(255, 30, 215, 96));
            repeatMode = 2;
        } else if (repeatMode == 2) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);    // no loop
            repeatB.setColorFilter(Color.argb(255, 67, 65, 73));
            repeatMode = 0;
        }
    }

    private void shuffleSong() {
        Context context = this;
        if (shuffle) {
            songList = songListOrigin;
            shuffleB.setColorFilter(Color.argb(255, 67, 65, 73));
//            Toast.makeText(context, "Unshuffle", Toast.LENGTH_SHORT).show();
            shuffle = false;
        } else {
            Collections.shuffle(songList);
            shuffleB.setColorFilter(Color.argb(255, 30, 215, 96));
//            Toast.makeText(context, "Shuffle", Toast.LENGTH_SHORT).show();
            shuffle = true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void playNextSong() {
        if (MediaPlayerService.songIndex == songList.size() - 1) {
            if (repeatMode == 1) {
                MediaPlayerService.songIndex = -1;
            } else {
                return;
            }
        }

        if (repeatMode != 2) {
            MediaPlayerService.songIndex++;
        }
        mediaPlayer.reset();
        setMusic();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void playPreviousSong() {
        if (MediaPlayerService.songIndex == 0) {
            if (repeatMode == 1) {
                MediaPlayerService.songIndex = songList.size();
            } else {
                return;
            }
        }
        if (repeatMode != 2) {
            MediaPlayerService.songIndex--;
        }
        mediaPlayer.reset();
        setMusic();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void forceNext() {
        repeatMode = 1;
        repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
        repeatB.setColorFilter(Color.argb(255, 30, 215, 96));

        playNextSong();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void forcePrevious() {
        repeatMode = 1;
        repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
        repeatB.setColorFilter(Color.argb(255, 30, 215, 96));

        playPreviousSong();
    }

    public String convertTime(String duration) {
        long time = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
    }

}