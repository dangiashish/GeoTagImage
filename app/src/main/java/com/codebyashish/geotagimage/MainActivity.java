package com.codebyashish.geotagimage;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements PermissionCallback {
    private ImageView ivCamera, ivImage, ivClose;
    private static String imageStoragePath;
    public static final String IMAGE_EXTENSION = ".png";
    private Uri fileUri;
    private static final int CAMERA_IMAGE_REQUEST_CODE = 2000;
    private static final int PERMISSION_REQUEST_CODE = 100;

    static FragmentActivity mContext;
    private GeoTagImage geoTagImage;
    private PermissionCallback permissionCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize the context
        mContext = MainActivity.this;
        // initialize the permission callback listener
        permissionCallback = this;

        // initialize the GeoTagImage class object with context and callback
        // use try/catch block to handle exceptions.
        try {
            geoTagImage = new GeoTagImage(mContext, permissionCallback);
        } catch (GTIException e) {
            throw new RuntimeException(e);
        }

        // initialize the xml buttons.
        ivCamera = findViewById(R.id.ivCamera);
        ivImage = findViewById(R.id.ivImage);
        ivClose = findViewById(R.id.ivClose);

        // setOnClickListener on camera button.
        ivCamera.setOnClickListener(click -> {
            // first check permission for camera and location by using GTIPermission class.
            if (GTIPermissions.checkCameraLocationPermission(mContext)) {

                // if permissions are granted, than open camera.
                openCamera();

            } else {
                // otherwise request for the permissions by using GTIPermission class.
                GTIPermissions.requestCameraLocationPermission(mContext, PERMISSION_REQUEST_CODE);
            }
        });
    }


    // if permissions are granted for camera and location.
    private void openCamera() {
        // call Intent for ACTION_IMAGE_CAPTURE which will redirect to device camera.
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // create a file object
        File file;

        // before adding GeoTags, generate or create an original image file
        // TODO-Note : we need to create an original image to add geotags by copying this file.
        file = GTIUtility.generateOriginalFile(mContext, IMAGE_EXTENSION);
        if (file != null) {
            // if file has been created, then will catch its path for future reference.
            imageStoragePath = file.getPath();
        }

        // now get Uri from this created image file by using GTIUtility.getFileUri() function.
        fileUri = GTIUtility.getFileUri(mContext, file);

        // pass this uri file into intent filters while opening camera.
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // call startActivityForResult method by passing the intent filter with a request code.
        startActivityForResult(intent, CAMERA_IMAGE_REQUEST_CODE);

    }


    // override the onActivityResult method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check the request code for the result
        if (requestCode == CAMERA_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                try {
                    // now call the function createImage() and pass the uri object (line no. 90-100)
                    geoTagImage.createImage(fileUri);

                    // set all the customizations for geotagging as per your requirements.
                    geoTagImage.setTextSize(30f);
                    geoTagImage.setBackgroundRadius(5f);
                    geoTagImage.setBackgroundColor(Color.parseColor("#66000000"));
                    geoTagImage.setTextColor(getColor(android.R.color.white));
                    geoTagImage.setAuthorName("Ashish");
                    geoTagImage.showAuthorName(true);
                    geoTagImage.showAppName(true);
                    geoTagImage.setImageQuality(ImageQuality.LOW);

                    // after geotagged photo is created, get the new image path by using getImagePath() method
                    imageStoragePath = geoTagImage.getImagePath();

                    /* The time it takes for a Canvas to draw items on a blank Bitmap can vary depending on several factors,
                     * such as the complexity of the items being drawn, the size of the Bitmap, and the processing power of the device.*/
                    new Handler().postDelayed(this::previewCapturedImage, 3000);


                } catch (GTIException e) {
                    throw new RuntimeException(e);
                }


                // handle the error or cancel events
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(mContext, "Cancelled image capture", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // preview of the original image
    private void previewCapturedImage() {
        try {
            ivCamera.setVisibility(View.GONE);
            Bitmap bitmap = GTIUtility.optimizeBitmap(imageStoragePath);
            Bitmap bitmap1 = BitmapFactory.decodeFile(imageStoragePath);
            ivImage.setImageBitmap(bitmap1);

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
                geoTagImage.handlePermissionGrantResult();
                Toast.makeText(mContext, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPermissionGranted() {
        openCamera();
    }

    @Override
    public void onPermissionDenied() {
        GTIPermissions.requestCameraLocationPermission(mContext, PERMISSION_REQUEST_CODE);
    }
}