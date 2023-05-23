package org.tensorflow.lite.examples.classification;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;
import org.tensorflow.lite.examples.classification.tflite.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.mukesh.MarkdownView;

public class GuideActivity extends AppCompatActivity {

    String TAG = "GuideActivity";

    String text;
    Bitmap img;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_guide_md);

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


        /*
        NestedScrollView mainScrollView = findViewById(R.id.scrollNestedView);
        mainScrollView.post(new Runnable() {
            public void run() {
                mainScrollView.fullScroll(View.FOCUS_UP);
            }
        });

        loadImageGuide("guides/" + monumentId + "/img.jpg");
        loadGuidefromFile("guides/" + monumentId + "/" + language + "/testo.txt");


        //String pathVideo = "android.resource://" + getPackageName() + "/";
        String pathVideo = "https://dl.dropboxusercontent.com/s/";
        //String pathAudio = "guides/" + monumentId + "/" + language + "/audio.mp3";
        String pathAudio = "https://dl.dropboxusercontent.com/s/";


         */

        /*
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

         */



        //TextView textTextView = findViewById(R.id.textGuide);
        //textTextView.setText(text);

        //TextView hintsTextView = findViewById(R.id.hints);
        //hintsTextView.setText(hints.toString());



        /*
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




         */

        MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdown_view);
        //markdownView.setMarkDownText("# Hello World\nThis is a simple markdown"); //Displays markdown text

        markdownView.loadMarkdownFromAssets("guides/" + monumentId + "/" + language + "/guide.md"); //Loads the markdown file from the assets folder


        //Show hints

        ArrayList<Element> hints = getHints(monumentId);

        Button p1_button = findViewById(R.id.hintButton1);
        p1_button.setText(hints.get(0).getMonument());
        Button p2_button = findViewById(R.id.hintButton2);
        p2_button.setText(hints.get(1).getMonument());
        Button p3_button = findViewById(R.id.hintButton3);
        p3_button.setText(hints.get(2).getMonument());



        //Wait few seconds to let the md file open

        View hintsView = findViewById(R.id.hintsView);

        final Runnable r = new Runnable() {
            public void run() {
                hintsView.setVisibility(View.VISIBLE);
            }
        };

        Handler handler = new Handler();
        handler.postDelayed(r, 2000);




    }

    private ArrayList<Element> getHints(String monumendId) { //hints just calculating the distances
        float[] recognizedVec = new float[0];
        ArrayList<Element> listDocToVec = DatabaseAccess.getListDocToVec();

        //find the vec of the recognized monument
        for (Element x:listDocToVec) {
            if(x.getMonument().equals(monumendId)){
                recognizedVec=x.getMatrix();
            }
        }

        //compute all distances
        for (Element x:listDocToVec) {
            x.setDistance(euclideanDistance(x.getMatrix(),recognizedVec));
        }

        //sort by distances
        Collections.sort(listDocToVec, new Comparator<Element>(){
            public int compare(Element obj1, Element obj2) {
                // ## Ascending order
                 return Double.compare(obj1.getDistance(), obj2.getDistance()); // To compare integer values
            }
        });
        Log.i(TAG, "listDocToVec: "+listDocToVec);

        ArrayList<Element> results = new ArrayList<>();
        results.add(listDocToVec.get(1));
        results.add(listDocToVec.get(2));
        results.add(listDocToVec.get(listDocToVec.size()-1));

        return results;
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



    public static double euclideanDistance(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions must be equal");
        }

        double sumOfSquares = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sumOfSquares += diff * diff;
        }

        return Math.sqrt(sumOfSquares);
    }
}