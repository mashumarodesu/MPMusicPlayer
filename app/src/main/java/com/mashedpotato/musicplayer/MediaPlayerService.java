package com.mashedpotato.musicplayer;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    public static MediaPlayer mediaPlayer;

    public static MediaPlayer getMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        return mediaPlayer;
    }

    private static final String CHANNEL_ID = "mpNotiPlayer";
//    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private int resumePos;

    // Handle incoming calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    // Binder
    private final IBinder iBinder = new LocalBinder();

    // List of available Audio files
    public static int songIndex = -1; // song position
    private ArrayList<Song> songList;
    private Song activeSong; // currently playing song

    // Media control
    public static final String ACTION_PLAY = "com.mashedpotato.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.mashedpotato.musicplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.mashedpotato.musicplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.mashedpotato.musicplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.mashedpotato.musicplayer.ACTION_STOP";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    public ArrayList<Song> getSongList() {
        return this.songList;
    }

    public void setSongList(ArrayList<Song> songList) {
        this.songList = songList;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        callStateListener();
        becomingNoisyRegister();
        register_playNewAudio();
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
            mediaPlayer.setDataSource(activeSong.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.prepareAsync();
    }

    public void playMedia() {
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    // Handles the initialization of the MediaPlayer and the focus request to make sure there are no other apps playing media
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // Load data from SharedPreferences
            Storage storage = new Storage(getApplicationContext());
            songList = storage.loadSong();
            songIndex = storage.loadSongIndex();

            if (songIndex != -1 && songIndex < songList.size()) {
                // Song index is in a valid range
                activeSong = songList.get(songIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        // Request audio focus
        if (!requestAudioFocus()) {
            // Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        // Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Release the mediaPlayer & remove Audio Focus
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();

        // Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        // Unregister BroadcastReceiver
        unregisterReceiver(becomingNoisy);
        unregisterReceiver(playNewAudio);

        // Stop the notification player
        removeNotification();

        // Clear cached playlist in SharedPreferences
        new Storage(getApplicationContext()).clearCachedPlaylist();
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
        @RequiresApi(api = Build.VERSION_CODES.O)
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
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {

            // Get the new media index form SharedPreferences
            songIndex = new Storage(getApplicationContext()).loadSongIndex();
            if (songIndex != -1 && songIndex < songList.size()) {
                // If index is in a valid range
                activeSong = songList.get(songIndex);
            } else {
                stopSelf();
            }

            // A PLAY_NEW_AUDIO action received, reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio() {
        // Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; // A mediaSessionManager already exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");

        // Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();

        // Set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);

        // Indicate that the MediaSession handles transport control commands through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                // Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_cover); // replace with song/album cover art
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeSong.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeSong.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeSong.getTitle())
//                .putString(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, activeSong.getTrackNum())
                .build());
    }

    private void skipToNext() {
        if (songIndex == songList.size() - 1) {
            // If current song is the last in playlist, set index to the first song
            songIndex = 0;
            activeSong = songList.get(songIndex);
        } else {
            // Get next in playlist
            activeSong = songList.get(++songIndex);
        }

        // Update stored index
        new Storage(getApplicationContext()).storeSongIndex(songIndex);

        stopMedia();
        // Reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skipToPrevious() {
        if (songIndex == 0) {
            // If current song is the first in playlist, set index to the last song
            songIndex = songList.size() - 1;
            activeSong = songList.get(songIndex);
        } else {
            // Get previous in playlist
            activeSong = songList.get(--songIndex);
        }

        // Update stored index
        new Storage(getApplicationContext()).storeSongIndex(songIndex);

        stopMedia();
        // Reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void buildNotification(PlaybackStatus playbackStatus) {

            CharSequence name = getString(R.string.notificationMediaPlayer);
            String description = getString(R.string.notificationMediaPlayerDescription);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            int notificationAction = android.R.drawable.ic_media_pause; // Needs to be initialized
            PendingIntent play_pauseAction = null;

            //Build a new notification according to the current state of the MediaPlayer
            if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_cover); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                // Attach MediaSession token
                .setMediaSession(mediaSession.getSessionToken())
                // Show playback controls in the compact notification view.
                .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.white))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(activeSong.getArtist())
                .setContentTitle(activeSong.getTitle())
                .setContentInfo("")
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private PendingIntent playbackAction(int actionNumber) {

        Intent playbackAction = new Intent(this, MediaPlayerService.class);

        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {

        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();

        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }
}
