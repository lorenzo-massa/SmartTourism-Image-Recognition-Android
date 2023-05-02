package org.tensorflow.lite.examples.classification;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GuideActivity extends AppCompatActivity {

    String TAG = "GuideActivity";

    String text;
    Bitmap img;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_guide);

        NestedScrollView mainScrollView = findViewById(R.id.scrollNestedView);
        mainScrollView.post(new Runnable() {
            public void run() {
                mainScrollView.fullScroll(View.FOCUS_UP);
            }
        });


        String monumentId = getIntent().getStringExtra("monument_id");
        String language = getIntent().getStringExtra("language");

        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);
        toolbar.setTitle(monumentId);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                stopAudio();
                finish();
            }
        });


        loadImageGuide("guides/" + monumentId + "/img.jpg");
        loadGuidefromFile("guides/" + monumentId + "/" + language + "/testo.txt");


        //String pathVideo = "android.resource://" + getPackageName() + "/";
        String pathVideo = "https://dl.dropboxusercontent.com/s/";
        //String pathAudio = "guides/" + monumentId + "/" + language + "/audio.mp3";
        String pathAudio = "https://dl.dropboxusercontent.com/s/";


        switch (monumentId) {
            case "Cattedrale Duomo":
                if (language.equals("English")) {
                    pathVideo += "ldt7ng0eaxog55s/duomo_english.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                } else {
                    pathVideo += "ee3n2s3uls7ryhv/duomo_italian.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                }
                break;
            case "Campanile Giotto":
                if (language.equals("English")) {
                    pathVideo += "kxxuxmdkyxgr9it/giotto_english.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                } else {
                    pathVideo += "3ivnh9a9lfeeyjs/giotto_italian.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                }
                break;
            case "Battistero SanGiovanni":
                if (language.equals("English")) {
                    pathVideo += "5lwaewd7bk86mlf/battistero_english.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                } else {
                    pathVideo += "g0pu1i6fsawkzmm/battistero_italian.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                }
                break;
            case "Loggia Bigallo":
                if (language.equals("English")) {
                    pathVideo += "ksabtl8jtaeftcb/loggia_english.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                } else {
                    pathVideo += "oxn2kxthnlgyd9a/loggia_italian.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                }
                break;
            case "Palazzo Vecchio":
                if (language.equals("English")) {
                    pathVideo += "ob5asd114e7o8jm/palazzo_english.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                } else {
                    pathVideo += "x44z7eckei4dysm/palazzo_italian.mp4";
                    pathAudio += "ujmvjjwy7s4iode/audio.mp3";
                }
                break;
            default:
                break;
        }

        TextView textTextView = findViewById(R.id.textGuide);
        textTextView.setText(text);

        ImageView imageImageView = findViewById(R.id.imgGuide);
        imageImageView.setImageBitmap(img);

        VideoView videoView = findViewById(R.id.videoView);
        Uri uriVideo = Uri.parse(pathVideo);
        videoView.setVideoURI(uriVideo);
        //videoView.setVideoPath(pathVideo);

        MediaController mediaController = new MediaController(this);
        //mediaController.setMediaPlayer(videoView);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        videoView.seekTo(5);
        //videoView.start();

        Button playBtn = findViewById(R.id.idBtnPlay);
        String finalPathAudio = pathAudio;
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio(finalPathAudio);
            }
        });
    }

    private void loadGuidefromFile(String fileName) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(fileName)));

            // do reading, usually loop until end of file reading
            String mLine;
            //while ((mLine = reader.readLine()) != null) {
            //process line
            //    Log.v(TAG, mLine);
            //}

            text = reader.readLine();
        } catch (IOException e) {
            //log the exception
            e.printStackTrace();
            text = "Error: Text guide not found!";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
    }

    private void loadImageGuide(String fileName) {
        AssetManager am = getAssets();
        InputStream is = null;
        try {
            is = am.open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        img = BitmapFactory.decodeStream(is);
    }

    private void playAudio(String pathAudio) {

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                //AssetFileDescriptor afd = getAssets().openFd(pathAudio);
                mediaPlayer.setDataSource(pathAudio);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Toast.makeText(GuideActivity.this, "Audio has been paused", Toast.LENGTH_SHORT).show();
        } else {
            mediaPlayer.start();
            Toast.makeText(GuideActivity.this, "Audio started playing..", Toast.LENGTH_SHORT).show();
        }

    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}