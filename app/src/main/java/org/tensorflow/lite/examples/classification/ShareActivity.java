package org.tensorflow.lite.examples.classification;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.tensorflow.lite.examples.classification.tflite.DatabaseAccess;
import org.tensorflow.lite.examples.classification.tflite.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

public class ShareActivity extends AppCompatActivity {

    private String monumentId;
    private Bitmap bitmap = null;
    ImageView imageView;
    private static final int pic_id = 123;
    private static final String TAG = "ShareActivity";

    ConstraintLayout constraintLayout_id;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        monumentId = getIntent().getStringExtra("monument_id");
        imageView = findViewById(R.id.actual_photo);
        constraintLayout_id = findViewById(R.id.constraintLayout);


        String link = DatabaseAccess.getImageLink(monumentId);

        //Save image from url
        Glide.with(ShareActivity.this)
                .load(link)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {

                        bitmap = ((BitmapDrawable)resource).getBitmap();
                        bitmap = addWatermark(bitmap);

                        //Show the image
                        imageView.setImageBitmap(bitmap);

                        if (bitmap.getWidth() < bitmap.getHeight()) {
                            //Vertical
                            imageView.setImageBitmap(
                                    BITMAP_RESIZER(bitmap,
                                            constraintLayout_id.getWidth(), constraintLayout_id.getHeight()));
                        }

                        imageView.requestLayout();

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);

                        Toast.makeText(ShareActivity.this, "Failed to Download Image! Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });

        Button button_share = findViewById(R.id.share_button);
        button_share.setOnClickListener(v -> {

            if (bitmap == null) {
                Toast.makeText(ShareActivity.this, "Please wait for the image to load!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Instantiate an intent
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            Uri uri = getmageToShare(bitmap);

            // Add the URI to the Intent.
            intent.putExtra(Intent.EXTRA_STREAM, uri);

            // Add extra text to the Intent (optional)
            intent.putExtra(Intent.EXTRA_TEXT, "I'm visiting " + monumentId + " with SmartTourism app!");
            intent.putExtra(Intent.EXTRA_TITLE, "SmartTourism");
            intent.putExtra(Intent.EXTRA_SUBJECT, "SmartTourism");

            // Broadcast the Intent.
            startActivity(Intent.createChooser(intent, "Share to"));
        });

        Button button_take_photo = findViewById(R.id.take_photo_button);
        button_take_photo.setOnClickListener(v -> {

            /* Low quality image
            Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(camera_intent, pic_id);
             */


            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
            imageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, pic_id);
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(view -> {
            onBackPressed();
            finish();
        });

    }

    // This method will help to retrieve the image
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Match the request 'pic id with requestCode
        if (requestCode == pic_id && resultCode == RESULT_OK) {

            try {
                bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                bitmap = addWatermark(bitmap);
                imageView.setImageBitmap(bitmap);
                //String imageurl = getRealPathFromURI(imageUri);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Resize the image to fit the screen
            if (bitmap.getWidth() < bitmap.getHeight()) {
                //Vertical
                imageView.setImageBitmap(
                        BITMAP_RESIZER(bitmap,
                                constraintLayout_id.getWidth(), constraintLayout_id.getHeight()));
            }

            imageView.requestLayout();
        }
    }


    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    // Retrieving the url to share
    private Uri getmageToShare(Bitmap bitmap) {
        File imagefolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imagefolder.mkdirs();
            File file = new File(imagefolder, "shared_image.jpeg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            uri = FileProvider.getUriForFile(ShareActivity.this, "org.tensorflow.lite.examples.classification.fileprovider", file);
        } catch (Exception e) {
            Toast.makeText(ShareActivity.this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return uri;
    }

    // Add watermark to image
    private Bitmap addWatermark(Bitmap src) { //TODO: Check because some times the logo is added wrongly
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        //Draw drawable
        Drawable drawable = getResources().getDrawable(R.drawable.logo_name);
        Bitmap bitmapLogo = ((BitmapDrawable)drawable).getBitmap();
        //Resize bitmap
        bitmapLogo = BITMAP_RESIZER(bitmapLogo, 50, 50);
        canvas.drawBitmap(bitmapLogo, w-60, h-60, new Paint(Paint.FILTER_BITMAP_FLAG));

        return result;
    }

    private Bitmap BITMAP_RESIZER(Bitmap bitmap,int newWidth,int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;

    }

}
