package com.mashedpotato.musicplayer;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    public static ArrayList<Song> songListFavorite = new ArrayList<>();

    private RecyclerView songRV;
    private SearchView searchSV;

    private int sortCategory = 0, sortOrder = 0;

    private Adapter adapter;
    private Storage storage;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.mashedpotato.musicplayer.PlayNewAudio";

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            int PERMISSION_CODE = 1;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
        }
        setContentView(R.layout.activity_main);

        ImageButton menuIB = findViewById(R.id.idIBMenu);
        songRV = findViewById(R.id.idRVSong);
        searchSV = findViewById(R.id.idSVSearch);
        ImageButton playerIB = findViewById(R.id.idIBPlayer);

        ImageView searchIcon = searchSV.findViewById(androidx.appcompat.R.id.search_button);
        searchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_search_24));

        runtimePerm();
        storage = new Storage(getApplicationContext());
        new MyAsyncTask().execute();

        menuIB.setOnClickListener(this::showOptionsMenu);

        playerIB.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
            MainActivity.this.startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.idMenuSearch);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.idMenuSearch) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            for (Song song : songList) {
                if (song.isFavorite()) {
                    songListFavorite.add(song);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            initSongRecyclerView();
            songListOrigin = (ArrayList<Song>) songList.clone();
            storage.storeAudio(songList);
        }
    }

    private void initSongRecyclerView() {
        if (songList.size() > 0) {
            songRV = findViewById(R.id.idRVSong);
            Collections.sort(songList, (o1, o2) -> {

                int sortResult = 0;

                switch (sortCategory) {
                    case 0:
                        sortResult = o1.getTitle().compareToIgnoreCase(o2.getTitle());
                        break;
                    case 1:
                        sortResult = o1.getArtist().compareToIgnoreCase(o2.getArtist());
                        break;
                    case 2:
                        sortResult = o1.getAlbum().compareToIgnoreCase(o2.getAlbum());
                        break;
                }

                switch (sortOrder) {
                    case 0:
                        return sortResult;
                    case 1:
                        return -sortResult;
                }
                return 0;
            });
            adapter = new Adapter(MainActivity.this, songList);
            songRV.setHasFixedSize(true);
            songRV.setAdapter(adapter);
            songRV.setLayoutManager(new LinearLayoutManager(this));
            songRV.addOnItemTouchListener(new TouchListener(this, (view, index) -> {
                playAudio(index);
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MainActivity.this.startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
            }));
        }

        searchSV.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                if (adapter != null) {
                    adapter.getFilter().filter(text);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                if (adapter != null) {
                    adapter.getFilter().filter(text);
                    return true;
                }
                return false;
            }
        });
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
                String songAlbum = cursor.getString(albumColumn);
                String songNum = cursor.getString(trackColumn);
                String songDuration = cursor.getString(durationColumn);

                Uri songUri = ContentUris.withAppendedId(uri, songId);

                // Save to audioList
                songList.add(new Song(songData, songTitle, songAlbum, songArtist, songNum, songUri.toString(), songDuration));
            }

            cursor.close();
        }

        Storage storage = new Storage(getApplicationContext());
        if (storage.loadSongFav() != null) {
            for (Song song : songList) {
                if (storage.loadSongFav().contains(song)) {
                    song.setFavorite(true);
                }
            }
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
            songRV.setAdapter(new Adapter(getApplicationContext(), songList));
        }
    }

    private void playAudio(int songIndex) {

        Intent playerIntent = new Intent(this, MediaPlayerService.class);

        //Check is service is active
        Storage storage = new Storage(getApplicationContext());
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            storage.storeAudio(songList);
            storage.storeSongIndex(songIndex);

            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            storage.storeSongIndex(songIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    public void showOptionsMenu(View view) {

        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater menuInflater = popupMenu.getMenuInflater();
        menuInflater.inflate(R.menu.recycle_view_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            RadioButton rb;
            switch (item.getItemId()) {
                // Theme
                case R.id.idLightTheme:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case R.id.idDarkTheme:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                // Sorting category
                case R.id.idTitleSort:
                    sortCategory = 0;
                    break;
                case R.id.idArtistSort:
                    sortCategory = 1;
                    break;
                case R.id.idAlbumSort:
                    sortCategory = 2;
                    break;
                // Sorting order
                case R.id.idAscending:
                    sortOrder = 0;
                    break;
                case R.id.idDescending:
                    sortOrder = 1;
                    break;
            }
            initSongRecyclerView();
            return true;
        });

        popupMenu.show();
    }
}