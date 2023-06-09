package com.codebyashish.geotagimage;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView ivCamera, ivImage, ivClose, ivGallery;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static String imageStoragePath;
    public static final int BITMAP_SAMPLE_SIZE = 4;
    public static final String GALLERY_DIRECTORY_NAME = "Camera";
    public static final String IMAGE_EXTENSION = "png";
    private Uri fileUri;

    private static final int CAMERA_IMAGE_REQUEST_CODE = 2000;
    private static final int PERMISSION_REQUEST_CODE = 100;

    static FragmentActivity mContext;
    private GeoTagImage geoTagImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = MainActivity.this;
        geoTagImage = new GeoTagImage(mContext);

        ivCamera = findViewById(R.id.ivCamera);
        ivGallery = findViewById(R.id.ivGallery);
        ivImage = findViewById(R.id.ivImage);
        ivClose = findViewById(R.id.ivClose);

        ivCamera.setOnClickListener(this);
        ivGallery.setOnClickListener(this);


    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ivCamera) {
            if (Permissions.checkCameraPermission(mContext)) {
                captureImage();
            } else {
                Permissions.requestCameraPermission(mContext, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (file != null) {
            imageStoragePath = file.getPath();
        }
        fileUri = Utility.getFileUri(mContext, file);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, CAMERA_IMAGE_REQUEST_CODE);

    }

    // original image
    public static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), GALLERY_DIRECTORY_NAME);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(GALLERY_DIRECTORY_NAME, "Oops! Failed create "
                        + GALLERY_DIRECTORY_NAME + " directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + "." + IMAGE_EXTENSION);
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                previewCapturedImage();

                geoTagImage.createImage(fileUri);
                geoTagImage.setTextSize(25f);
                geoTagImage.setBackgroundRadius(5f);
                geoTagImage.setBackgroundColor(Color.parseColor("#66000000"));
                geoTagImage.setTextColor(getColor(android.R.color.white));
                imageStoragePath = geoTagImage.getImagePath();



            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(mContext, "User cancelled image capture", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void previewCapturedImage() {
        try {
            ivCamera.setVisibility(View.GONE);
            ivGallery.setVisibility(View.GONE);
            Bitmap bitmap = Utility.optimizeBitmap(BITMAP_SAMPLE_SIZE, imageStoragePath);
            ivImage.setImageBitmap(bitmap);

            if (ivImage.getDrawable() != null) {
                ivClose.setVisibility(View.VISIBLE);
            }
            ivClose.setOnClickListener(v -> {
                ivImage.setImageBitmap(null);
                ivCamera.setVisibility(View.VISIBLE);

                ivClose.setVisibility(View.GONE);
                ivImage.setImageDrawable(null);

            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mContext, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}