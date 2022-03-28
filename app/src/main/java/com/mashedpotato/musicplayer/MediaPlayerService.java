package com.mashedpotato.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,

        AudioManager.OnAudioFocusChangeListener {

    private final IBinder iBinder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private String mediaFile;
    private int resumePos;

    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    // Initialize MediaPlayer
    private void initMediaPlayer() {

        mediaPlayer = new MediaPlayer();

        // Setup MediaPlayer event listeners
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        // Reset the MediaPlayer
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set mediaFile location as the data source
            mediaPlayer.setDataSource(mediaFile);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.prepareAsync();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePos = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePos);
            mediaPlayer.start();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        // Invoked when playback of a media source has completed
        stopMedia();
        // Stop the service
        stopSelf();
    }

    @Override
    // This method handles errors
    public boolean onError(MediaPlayer mediaPlayer, int error, int detail) {
        // Invoked when there has been an error during an asynchronous operation
        switch (error) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + detail);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + detail);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + detail);
                break;
            default:
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //Invoked when the media source is ready for playback
        playMedia();
    }

    @Override
    public void onAudioFocusChange(int focus) {
        //Invoked when the audio focus of the system is updated
        switch (focus) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // The service gained audio focus, so it needs to start playing
                if (mediaPlayer == null) {
                    initMediaPlayer();
                } else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // User probably moved to playing media on another app, stop playback and release the media player
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time like when user receives a call, stop media but not release the media player because it is likely to resume
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time like when user receives a notification, keep playing but at an attenuated level
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;

            default:
        }
    }

    @Override
    // Handles the initialization of the MediaPlayer and the focus request to make sure there are no other apps playing media
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // An audio file is passed to the service through putExtra()
            mediaFile = intent.getExtras().getString("media");
        } catch (Exception e) {
            stopSelf();
        }

        // Request audio focus
        if (!requestAudioFocus()) {
            // Focus request is not granted
            stopSelf();
        }

        // If the mediaFile is valid, initialize the MediaPlayer
        if (mediaFile != null && !mediaFile.equals(""))
            initMediaPlayer();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
    }

    private boolean requestAudioFocus() {

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        // If focus request is granted, return true, else, return false
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    // Broadcast Receiver
    private BroadcastReceiver becomingNoisy = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    // Register the Broadcast Receiver
    private void becomingNoisyRegister() {
        // Register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisy, intentFilter);
    }

    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // Start listening for CallState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }
}
