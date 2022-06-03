package com.mashedpotato.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private MediaPlayerService player;
    boolean serviceBound = false;
    public ArrayList<Song> songList;
    private ArrayList<Song> songListOrigin;

    private ConstraintLayout homeCL;
    private TextInputEditText songEdt;
    private ImageView searchIV;
    private RecyclerView songRV;
    private TextView songTV, artistTV;
    private Button shuffleB;
    private BottomNavigationView bottomNavigationView;

    private Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private int PERMISSION_CODE = 1;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.mashedpotato.musicplayer.PlayNewAudio";

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
        }
        setContentView(R.layout.activity_main);

        homeCL = findViewById(R.id.idCLHome);
        songEdt = findViewById(R.id.idEdtSong);
        searchIV = findViewById(R.id.idIVSearch);
        songRV = findViewById(R.id.idRVSong);
        songTV = findViewById(R.id.idTVSong);
        artistTV = findViewById(R.id.idTVArtist);
        shuffleB = findViewById(R.id.idBShuffleMain);
        bottomNavigationView = findViewById(R.id.idBNVNavigation);

        runtimePerm();
        new MyAsyncTask().execute();
//        updateRecycleView();

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.idBNVISongs:

                        break;
                    case R.id.idBNVIArtists:

                        break;
                    case R.id.idBNVIAlbums:

                        break;
                    case R.id.idBNVIPlaylists:

                        break;
                }
                return true;
            }
        });

        shuffleB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PlayerActivity.shuffle) {
                    songList = songListOrigin;
                    shuffleB.setBackgroundResource(R.drawable.ic_baseline_shuffle_24);
                    PlayerActivity.shuffle = false;
                } else {
                    Collections.shuffle(songList);
                    shuffleB.setBackgroundResource(R.drawable.ic_baseline_shuffle_24);
                    PlayerActivity.shuffle = true;
                }
            }
        });

//        searchIV.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String song = songEdt.getText().toString();
//                if(song.isEmpty()) {
//                    Toast.makeText(MainActivity.this, "Enter a song here >:^l", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
    }

    public void updateRecycleView() {
        songRV.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(MainActivity.this);
        songRV.setLayoutManager(layoutManager);
        adapter = new Adapter(MainActivity.this, songList);
        songRV.setAdapter(adapter);
    }

    // Check if permission is granted or not
    public void runtimePerm() {
        Dexter.withContext(this).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    private class MyAsyncTask extends AsyncTask<Void, Void, Void> {
        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        protected Void doInBackground(Void... params) {
            loadAudio();
            songListOrigin = (ArrayList<Song>) songList.clone();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            initRecyclerView();
        }
    }

    private void initRecyclerView() {
        if (songList.size() > 0) {
            songRV = findViewById(R.id.idRVSong);
            songTV = findViewById(R.id.idTVSong);
            artistTV = findViewById(R.id.idTVArtist);
            Adapter adapter = new Adapter(MainActivity.this, songList);
            songRV.setHasFixedSize(true);
            songRV.setAdapter(adapter);
            songRV.setLayoutManager(new LinearLayoutManager(this));
            songRV.addOnItemTouchListener(new TouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    playAudio(index);
                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    intent.putExtra("List", songList);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    MainActivity.this.startActivity(intent);
                }
            }));

        }
    }

    // Search the storage for songs
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            songList = new ArrayList<>();

            // Caching column indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

            while (cursor.moveToNext()) {
                long songId = cursor.getLong(idColumn);
                String songData = cursor.getString(dataColumn);
                String songTitle = cursor.getString(titleColumn);
                String songArtist = cursor.getString(artistColumn);
                String songAlbum  = cursor.getString(albumColumn);
                String songNum = cursor.getString(trackColumn);
                String songDuration = cursor.getString(durationColumn);

                Uri songUri = ContentUris.withAppendedId(uri, songId);

                // Save to audioList
                songList.add(new Song(songData, songTitle, songAlbum, songArtist, songNum, songUri.toString(), songDuration));
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

    @Override
    protected void onResume() {
        super.onResume();
        if (songRV != null) {
            songRV.setAdapter( new Adapter(getApplicationContext(), songList));
        }
    }

    private void playAudio(int songIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            Storage storage = new Storage(getApplicationContext());
            storage.storeAudio(songList);
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