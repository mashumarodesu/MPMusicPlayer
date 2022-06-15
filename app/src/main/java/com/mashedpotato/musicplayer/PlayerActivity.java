package com.mashedpotato.musicplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    MediaPlayerService mService;
    boolean mBound = false;

    public static boolean needUpdate = true;

    private TextView playingSongTV, artistTV, currentTimeTV, totalTimeTV;
    private ImageView coverIV;
    private SeekBar seekBarSB;
    private ImageButton playB, previousB, nextB, repeatB, shuffleB;

    // 0 is no loop
    // 1 is loop all songs
    // 2 is loop one only
    private int repeatMode = 0;
    public static boolean shuffle = false;

    private Song song;
//    MediaPlayer mediaPlayer = MediaPlayerService.getMediaPlayer();

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

//        songList = (ArrayList<Song>) getIntent().getSerializableExtra("List");
//        songListOrigin = (ArrayList<Song>) songList.clone();

        setMusic();

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void run() {
                if (MediaPlayerService.mediaPlayer != null) {
                    seekBarSB.setProgress(MediaPlayerService.mediaPlayer.getCurrentPosition());
                    SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss");
                    currentTimeTV.setText(timeFormat.format(MediaPlayerService.mediaPlayer.getCurrentPosition()));

                    if (MediaPlayerService.mediaPlayer.isPlaying()) {
                        playB.setImageResource(R.drawable.ic_baseline_pause_24);
                    } else {
                        playB.setImageResource(R.drawable.ic_baseline_play_24);
                    }

                    if (needUpdate) {
                        setMusic();
                        needUpdate = false;
                    }
                }
                new Handler().postDelayed(this, 100);
            }
        });

        seekBarSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (MediaPlayerService.mediaPlayer != null && fromUser) {
                    MediaPlayerService.mediaPlayer.seekTo(progress);
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

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(serviceConnection);
        mBound = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void setMusic() {

        ContentResolver contentResolver = getContentResolver();

        Storage storage = new Storage(getApplicationContext());
        song = storage.loadSong().get(MediaPlayerService.songIndex);

        playingSongTV.setText(song.getTitle());
        artistTV.setText(song.getArtist());
        totalTimeTV.setText(convertTime(song.getDuration()));

        try {
            Bitmap cover = contentResolver.loadThumbnail(Uri.parse(song.getUriString()), new Size(500, 500), null);
            coverIV.setImageBitmap(cover);
        } catch (IOException e) {
            e.printStackTrace();
        }

        playB.setOnClickListener(v -> pauseSong());
        nextB.setOnClickListener(view -> forceNext());
        previousB.setOnClickListener(v -> forcePrevious());
        shuffleB.setOnClickListener(v -> shuffleSong());
        repeatB.setOnClickListener(v -> repeatSong());

        resetSeekBar();
    }

    private void resetSeekBar() {
        seekBarSB.setProgress(0);
        seekBarSB.setMax(MediaPlayerService.mediaPlayer.getDuration());
    }

    private void pauseSong() {
        mService.toggleMedia();
    }

    private void repeatSong() {
        if (repeatMode == 0) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
            repeatB.setColorFilter(R.color.accent1);
            repeatMode = 1;
        } else if (repeatMode == 1) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_one_24);    // to loop one song
            repeatB.setColorFilter(R.color.accent1);
            repeatMode = 2;
        } else if (repeatMode == 2) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);    // no loop
            repeatB.setColorFilter(R.color.accent2);
            repeatMode = 0;
        }
    }

    private void shuffleSong() {
        if (shuffle) {
            mService.shuffleSong(false);
            shuffleB.setColorFilter(Color.argb(255, 67, 65, 73));
            shuffle = false;
        } else {
            mService.shuffleSong(true);
            shuffleB.setColorFilter(Color.argb(255, 30, 215, 96));
            shuffle = true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void forceNext() {
        if (repeatMode == 2) {
            repeatMode = 1;
//            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
//            repeatB.setColorFilter(Color.argb(255, 30, 215, 96));
        }

        mService.skipToNext();
//        MediaPlayerService.mediaPlayer.reset();
        setMusic();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void forcePrevious() {
        if (repeatMode == 2) {
            repeatMode = 1;
//            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
//            repeatB.setColorFilter(Color.argb(255, 30, 215, 96));
        }

        mService.skipToPrevious();
//        MediaPlayerService.mediaPlayer.reset();
        setMusic();
    }

    public String convertTime(String duration) {
        long time = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
    }

    // Bind this client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

}