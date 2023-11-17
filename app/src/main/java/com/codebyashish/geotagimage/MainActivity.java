package com.codebyashish.geotagimage;

import static com.codebyashish.geotagimage.GeoTagImage.PNG;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity implements PermissionCallback {
    private ImageView ivCamera, ivImage, ivClose;
    private TextView tvOriginal, tvGtiImg;
    private static String originalImgStoragePath, gtiImageStoragePath;
    public static final String IMAGE_EXTENSION = ".png";
    private Uri fileUri;
    private static final int PERMISSION_REQUEST_CODE = 100;

    static FragmentActivity mContext;
    private GeoTagImage geoTagImage;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize the xml buttons.
        ivCamera = findViewById(R.id.ivCamera);
        ivImage = findViewById(R.id.ivImage);
        ivClose = findViewById(R.id.ivClose);
        progressBar = findViewById(R.id.progressBar);
        tvOriginal = findViewById(R.id.tvOriginalPath);
        tvGtiImg = findViewById(R.id.tvGTIPath);
        AppCompatButton btnGit = findViewById(R.id.btnGithub);

        btnGit.setOnClickListener(c -> {
            String url = "https://github.com/dangiashish/GeoTagImage";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);

        });




        // initialize the context
        mContext = MainActivity.this;
        // initialize the permission callback listener
        PermissionCallback permissionCallback = this;

        // initialize the GeoTagImage class object with context and callback
        // use try/catch block to handle exceptions.
        try {
            geoTagImage = new GeoTagImage(mContext, permissionCallback);
        } catch (GTIException e) {
            throw new RuntimeException(e);
        }

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
        // We need to create an original image to add geotags by copying this file.
        file = GTIUtility.generateOriginalFile(mContext, IMAGE_EXTENSION);
        if (file != null) {
            // if file has been created, then will catch its path for future reference.
            gtiImageStoragePath = file.getPath();
            originalImgStoragePath = file.getPath();
        }

        // now get Uri from this created image file by using GTIUtility.getFileUri() function.
        fileUri = GTIUtility.getFileUri(mContext, file);

        // pass this uri file into intent filters while opening camera.
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // call ActivityResultLauncher by passing the intent request.
        activityResultLauncher.launch(intent);

    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Handle the result here

                    try {
                        progressBar.setVisibility(View.VISIBLE);
                        ivCamera.setVisibility(View.GONE);

                        // TODO : START THE MAIN FUNCTIONALITY

                        // now call the function createImage() and pass the uri object (line no. 100-110)
                        geoTagImage.createImage(fileUri);

                        // set all the customizations for geotagging as per your requirements.
                        geoTagImage.setTextSize(30f);
                        geoTagImage.setBackgroundRadius(5f);
//                        geoTagImage.setBackgroundColor(Color.parseColor("#66000000"));
                        geoTagImage.setTextColor(android.R.color.white);
                        geoTagImage.setAuthorName("Ashish");
                        geoTagImage.showAuthorName(true);
                        geoTagImage.showAppName(true);
                        geoTagImage.setImageQuality(ImageQuality.LOW);
                        geoTagImage.setImageExtension(PNG);

                        // after geotagged photo is created, get the new image path by using getImagePath() method
                        gtiImageStoragePath = geoTagImage.getImagePath();

                        /* The time it takes for a Canvas to draw items on a blank Bitmap can vary depending on several factors,
                         * such as the complexity of the items being drawn, the size of the Bitmap, and the processing power of the device.*/
                        new Handler().postDelayed(this::previewCapturedImage, 3000);


                    } catch (GTIException e) {
                       e.printStackTrace();
                    }
                }
            });



    // preview of the original image
    private void previewCapturedImage() {
        try {
            Bitmap bitmap = GTIUtility.optimizeBitmap(gtiImageStoragePath);
            ivImage.setImageBitmap(bitmap);

            if (ivImage.getDrawable() != null) {
                ivClose.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
            ivClose.setOnClickListener(v -> {
                ivImage.setImageBitmap(null);
                ivCamera.setVisibility(View.VISIBLE);
                ivClose.setVisibility(View.GONE);
                ivImage.setImageDrawable(null);
            });

            tvGtiImg.setText(gtiImageStoragePath);
            tvOriginal.setText(originalImgStoragePath);

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
