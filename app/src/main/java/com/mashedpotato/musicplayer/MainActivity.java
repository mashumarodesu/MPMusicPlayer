package com.mashedpotato.musicplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MediaPlayerService player;
    boolean serviceBound = false;
    public ArrayList<Song> songsList;

    private RelativeLayout homeRL;
    private TextInputEditText songEdt;
    private ImageView searchIV;
    private RecyclerView songRV;
    private TextView songTV, artistTV;

    private Adapter adapter;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.mashedpotato.musicplayer.PlayNewAudio";

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeRL = findViewById(R.id.idRLHome);
        songEdt = findViewById(R.id.idEdtSong);
        searchIV = findViewById(R.id.idIVSearch);
        songRV = findViewById(R.id.idRVSong);
        songTV = findViewById(R.id.idTVSong);
        artistTV = findViewById(R.id.idTVArtist);

        loadAudio();

        adapter = new Adapter(this, songsList);
        songRV.setAdapter(adapter);

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String song = songEdt.getText().toString();
                if(song.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a song here >:^l", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Check if permission is granted or not
//    public void runtimePerm() {
//        Dexter.withContext(this).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE).withListener(new PermissionListener() {
//            @Override
//            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
//
//            }
//
//            @Override
//            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
//
//            }
//
//            @Override
//            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
//                permissionToken.continuePermissionRequest();
//            }
//        }).check();
//    }

    // Search the storage for songs
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            songsList = new ArrayList<>();
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                @SuppressLint("Range") String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                @SuppressLint("Range") String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                @SuppressLint("Range") String genre = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.GENRE));
                @SuppressLint("Range") String trackNum = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));

                // Save to audioList
                songsList.add(new Song(data, title, album, artist, genre, trackNum));
            }

            cursor.close();
        }
    }

    // Bind this client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            // Notify that service is bound
            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    // These methods save and restore the state of the serviceBound variable and unbind the Service when a user closes the app, which can prevent crashes
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }

    private void playAudio(int songIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            Storage storage = new Storage(getApplicationContext());
            storage.storeAudio(songsList);
            storage.storeSongIndex(songIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            Storage storage = new Storage(getApplicationContext());
            storage.storeSongIndex(songIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }
}