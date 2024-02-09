package org.tensorflow.lite.examples.classification;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.File;
import java.io.FileOutputStream;

public class ShareActivity extends AppCompatActivity {

    private static final String TAG = "ShareActivity";
    private static final int pic_id = 123;
    ImageView imageView;
    Uri imageUri;
    ConstraintLayout constraintLayout_id;
    private String monumentId;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout
        setContentView(R.layout.activity_share);

        // Get the monument id from the previous activity
        monumentId = getIntent().getStringExtra("monument_id");

        // Get the image view and the constraint layout
        imageView = findViewById(R.id.actual_photo);
        constraintLayout_id = findViewById(R.id.constraintLayout);

        // Get the image link from the database
        String link = DatabaseAccess.getImageLink(monumentId);

        // Load image from link using Glide
        Glide.with(ShareActivity.this)
                .load(link)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {

                        // Convert drawable to bitmap
                        bitmap = ((BitmapDrawable) resource).getBitmap();

                        // Adjust the aspect ratio of the image (for instagram
                        if (bitmap.getWidth() < bitmap.getHeight())
                            bitmap = adjustAspectRatio(bitmap, 0.5625f);

                        // Add watermark (logo) to image
                        bitmap = addWatermark(bitmap);

                        //Show the image
                        imageView.setImageBitmap(bitmap);

                        // Resize the image to fit the screen
                        if (bitmap.getWidth() < bitmap.getHeight()) {
                            //Vertical
                            imageView.setImageBitmap(
                                    BITMAP_RESIZER(bitmap,
                                            constraintLayout_id.getWidth(), constraintLayout_id.getHeight()));
                        }

                        // Reload the image
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

        // Share button
        Button button_share = findViewById(R.id.share_button);
        button_share.setOnClickListener(v -> {

            // Check if the image is loaded
            if (bitmap == null) {
                Toast.makeText(ShareActivity.this, "Please wait for the image to load!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Instantiate an intent
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            Uri uri = getImageToShare(bitmap);

            // Add the URI to the Intent.
            intent.putExtra(Intent.EXTRA_STREAM, uri);

            // Add extra text to the Intent (optional)
            intent.putExtra(Intent.EXTRA_TEXT, "I'm visiting " + monumentId + " with SmartTourism app!");
            intent.putExtra(Intent.EXTRA_TITLE, "SmartTourism");
            intent.putExtra(Intent.EXTRA_SUBJECT, "SmartTourism");

            // Broadcast the Intent.
            startActivity(Intent.createChooser(intent, "Share to"));
        });

        // Take photo button
        Button button_take_photo = findViewById(R.id.take_photo_button);
        button_take_photo.setOnClickListener(v -> {

            // Create parameters for Intent
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");

            // Where to store the resulting picture
            imageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Create the camera_intent ACTION_IMAGE_CAPTURE
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Choose the path where the image will be saved
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, pic_id);
        });

        // Back button
        Toolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(view -> {

            // Go back to the previous activity
            onBackPressed();

            // Close this activity
            finish();
        });

    }

    // This method retrieves the image
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Match the request 'pic id with requestCode
        if (requestCode == pic_id && resultCode == RESULT_OK) {

            try {
                // Get the image from data
                bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);

                // Adjust the aspect ratio of the image (for instagram
                if (bitmap.getWidth() < bitmap.getHeight())
                    bitmap = adjustAspectRatio(bitmap, 0.5625f);

                // Add watermark (logo) to image
                bitmap = addWatermark(bitmap);

                //Show the image
                imageView.setImageBitmap(bitmap);

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

            // Reload the image
            imageView.requestLayout();
        }
    }


    // Retrieving the url to share
    private Uri getImageToShare(Bitmap bitmap) {
        // Get the image folder
        File imagefolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            // Create the folder if it doesn't exist
            imagefolder.mkdirs();

            // Create the file
            File file = new File(imagefolder, "shared_image.jpeg");
            FileOutputStream outputStream = new FileOutputStream(file);

            // Compress the image
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            // Flush and close the output stream
            outputStream.flush();
            outputStream.close();

            // Get the new URI
            uri = FileProvider.getUriForFile(ShareActivity.this, "org.tensorflow.lite.examples.classification.fileprovider", file);

        } catch (Exception e) {
            Toast.makeText(ShareActivity.this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return uri;
    }

    // Add watermark to image
    private Bitmap addWatermark(Bitmap src) {
        float scaleFactor = 0.2f;

        //Get the image dimensions
        int w = src.getWidth();
        int h = src.getHeight();

        //Create result bitmap
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);

        //Draw background
        canvas.drawBitmap(src, 0, 0, null);

        //Get logo drawable
        Drawable drawable = getResources().getDrawable(R.drawable.logo_name);
        Bitmap bitmapLogo = ((BitmapDrawable) drawable).getBitmap();

        //Resize bitmap logo with a size relative to the image
        int new_size;
        if (w < h) // Vertical
            new_size = (int) (w * scaleFactor);
        else // Horizontal
            new_size = (int) (h * scaleFactor);

        bitmapLogo = BITMAP_RESIZER(bitmapLogo, new_size, new_size);

        //Draw Logo
        canvas.drawBitmap(bitmapLogo, w - new_size - 30, h - new_size - 30, new Paint(Paint.FILTER_BITMAP_FLAG));

        return result;
    }

    // Resize the image
    private Bitmap BITMAP_RESIZER(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        // Calculate the scale
        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        // Create the matrix
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        // Create the canvas
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);

        // Draw the bitmap
        canvas.drawBitmap(bitmap, middleX - (bitmap.getWidth() / 2), middleY - (bitmap.getHeight() / 2), new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;

    }

    private Bitmap adjustAspectRatio(Bitmap bitmap, float ratio) {
        int height = bitmap.getHeight();
        return BITMAP_RESIZER(bitmap, (int) (height * ratio), height);
    }

}
