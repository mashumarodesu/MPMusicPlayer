package com.mashedpotato.musicplayer;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{

    MediaPlayerService mService;
    boolean mBound = false;

    private GestureDetectorCompat gestureDetector;

    public static boolean needUpdate = true;

    private TextView playingSongTV, artistTV, currentTimeTV, totalTimeTV;
    private ImageView coverIV;
    private SeekBar seekBarSB;
    private ImageButton playB, previousB, nextB, repeatB, shuffleB, backB;

    // 0 is no loop
    // 1 is loop all songs
    // 2 is loop one only
    public static int repeatMode = 0;
    public static boolean shuffle = false;

    private Song song;

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    @SuppressLint("ClickableViewAccessibility")
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
        backB = findViewById(R.id.idIBMain);

        playingSongTV.setSelected(true);

        backB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBack();
            }
        });

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

        this.gestureDetector = new GestureDetectorCompat(this, this);
        gestureDetector.setOnDoubleTapListener(this);

        coverIV.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
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
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(serviceConnection);
        mBound = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
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
        seekBarSB.setMax(MediaPlayerService.mediaPlayer.getDuration());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pauseSong() {
        mService.toggleMedia();
    }

    private void repeatSong() {
        if (repeatMode == 0) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
            repeatB.setColorFilter(R.color.accent3);
            repeatMode = 1;
        } else if (repeatMode == 1) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_one_24);    // to loop one song
            repeatB.setColorFilter(R.color.accent3);
            repeatMode = 2;
        } else if (repeatMode == 2) {
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);    // no loop
            repeatB.setColorFilter(R.color.accent1);
            repeatMode = 0;
        }
    }

    private void shuffleSong() {
        if (shuffle) {
            mService.shuffleSong(false);
            shuffleB.setColorFilter(R.color.accent1);
            shuffle = false;
        } else {
            mService.shuffleSong(true);
            shuffleB.setColorFilter(R.color.accent3);
            shuffle = true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void forceNext() {
        if (repeatMode == 2) {
            repeatMode = 1;
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
        }

        mService.skipToNext();
        setMusic();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void forcePrevious() {
        if (repeatMode == 2) {
            repeatMode = 1;
            repeatB.setImageResource(R.drawable.ic_baseline_repeat_24);  // to loop all songs
        }

        mService.skipToPrevious();
        setMusic();
    }

    private void goBack() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PlayerActivity.this.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
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

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        pauseSong();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            forceNext();
        }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            forcePrevious();
        }

        if(e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
            goBack();
        }

        return true;
    }
}