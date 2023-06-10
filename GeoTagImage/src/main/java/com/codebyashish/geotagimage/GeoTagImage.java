package com.codebyashish.geotagimage;

import static com.codebyashish.geotagimage.ImageQuality.HIGH;
import static com.codebyashish.geotagimage.ImageQuality.STANDARD;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 2023, Copyright by Ashish Dangi,
 * <a href="https://github.com/dangiashish">github.com/dangiashish</a>,
 * India
 */

public class GeoTagImage {
    private String place = "", road = "", latlng = "", date = "";
    private int originalImageHeight = 0;
    private int originalImageWidth = 0;
    private Context context;
    private File returnFile;
    private Bitmap bitmap = null, mapBitmap = null;
    private List<Address> addresses;
    private static final String IMAGE_EXTENSION = ".jpg";
    private Uri fileUri;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;
    private double latitude, longitude;
    private float textSize, textTopMargin;
    private Typeface typeface;
    private float radius;
    private int backgroundColor, textColor;
    private float backgroundHeight, backgroundLeft;
    private String authorName;
    private boolean showAuthorName, showAppName, showLatLng, showDate, showGoogleMap;
    private ArrayList<String> elementsList = new ArrayList<>();
    private int mapHeight, mapWidth, bitmapWidth, bitmapHeight;
    private String apiKey, center, imageUrl, dimension, markerUrl, imageQuality;
    private final PermissionCallback permissionCallback;


    public GeoTagImage(Context context, PermissionCallback callback) throws GTIException {

        if (context == null) {
            throw new GTIException("Context is not initialized or assigned to null value, use : context = getActivity();");
        }
        if (callback == null) {
            throw new GTIException("Permission callback is not initialized or assigned to null value, use : permissionCallback = this;");
        }

        this.context = context;
        this.permissionCallback = callback;


    }

    public void createImage(Uri fileUri) throws GTIException {

        if (fileUri == null) {
            throw new GTIException("Uri cannot be null");
        }

        this.fileUri = fileUri;

        // set default values here.
        textSize = 25f;
        typeface = Typeface.DEFAULT;
        radius = dpToPx(6);
        backgroundColor = Color.parseColor("#66000000");
        textColor = context.getColor(android.R.color.white);
        backgroundHeight = 150f;
        authorName = "";
        showAuthorName = false;
        showAppName = false;
        showGoogleMap = true;
        showLatLng = true;
        showDate = true;
        mapHeight = (int) backgroundHeight;
        mapWidth = 120;
        imageQuality = null;

        initialization();
    }

    private void initialization() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);


        if (imageQuality == null) {
            bitmapWidth = 960;
            bitmapHeight = 1280;
            textTopMargin = 50;
        }


        getDeviceLocation();
//        getDimensions();


    }

    private void getDeviceLocation() {
        if (GTIPermissions.checkCameraLocationPermission(context)) {
            Task<Location> task = fusedLocationProviderClient.getLastLocation();
            task.addOnSuccessListener(location -> {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    geocoder = new Geocoder(context, Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        if (GTIUtility.isGoogleMapsLinked(context)) {
                            if (GTIUtility.getMapKey(context) == null) {
                                Bitmap bitmap = createBitmap();
                                storeBitmapInternally(bitmap);

                                throw new GTIException("API key not found for this project");
                            } else {

                                apiKey = GTIUtility.getMapKey(context);

                                center = latitude + "," + longitude;
                                dimension = mapWidth + "x" + mapHeight;
                                markerUrl = String.format(Locale.getDefault(), "%s%s%s", "markers=color:red%7C", center, "&");
                                imageUrl = String.format(Locale.getDefault(), "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%d&size=%s&%s&maptype=%s&key=%s",
                                        center, 15, dimension, markerUrl, "satellite", apiKey);

                                new LoadMapImageTask().execute(imageUrl);
                            }
                        } else if (!GTIUtility.isGoogleMapsLinked(context)) {
                            Bitmap bitmap = createBitmap();
                            storeBitmapInternally(bitmap);
                            throw new GTIException("Project is not linked with google map sdk");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("error ", Objects.requireNonNull(e.getMessage()));

                    }
                }
            });
        }
    }

    private class LoadMapImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            String imageUrl = params[0];

            try {
                InputStream inputStream = new URL(imageUrl).openStream();
                return BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                mapBitmap = result;
                Bitmap bitmap = createBitmap();
                storeBitmapInternally(bitmap);
            }
        }
    }

    public void getDimensions() throws GTIException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(fileUri), null, options);
            originalImageHeight = options.outHeight;
            originalImageWidth = options.outWidth;
            Log.d("ASHISH", originalImageHeight + " & " + originalImageWidth);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new GTIException("File Not Found : " + e.getMessage());
        }
    }

    private Bitmap createBitmap() {
        Bitmap b = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawARGB(0, 255, 255, 255);
        canvas.drawRGB(255, 255, 255);
        copyTheImage(canvas);
        return b;
    }

    private void copyTheImage(Canvas canvas) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Paint design = new Paint();
        Bitmap scaledbmp = Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false);
        canvas.drawBitmap(scaledbmp, 0, 0, design);

        Paint rectPaint = new Paint();
        rectPaint.setColor(backgroundColor);
        rectPaint.setStyle(Paint.Style.FILL);

        if (showAuthorName) {
            backgroundHeight = backgroundHeight + textTopMargin;
        }
        if (showDate) {
            backgroundHeight = backgroundHeight + textTopMargin;
        }
        if (showLatLng) {
            backgroundHeight = backgroundHeight + textTopMargin;
        }


        if (GTIUtility.isGoogleMapsLinked(context)) {
            if (mapBitmap != null) {
                if (showGoogleMap) {
                    float mapLeft = 10;
                    backgroundLeft = mapBitmap.getWidth() + 20;
                    canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10, canvas.getHeight() - 10, dpToPx(radius), dpToPx(radius), rectPaint);
                    Bitmap scaledbmp2 = Bitmap.createScaledBitmap(mapBitmap, mapWidth, mapHeight, false);
                    canvas.drawBitmap(scaledbmp2, mapLeft, (canvas.getHeight() - backgroundHeight) + (backgroundHeight - mapBitmap.getHeight()) / 2, design);

                    float textX = backgroundLeft + 10;
                    float textY = canvas.getHeight() - (backgroundHeight - textTopMargin);

                    drawText(textX, textY, canvas);
                } else {
                    backgroundLeft = 10;
                    canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10, canvas.getHeight() - 10, dpToPx(radius), dpToPx(radius), rectPaint);

                    float textX = backgroundLeft + 10;
                    float textY = canvas.getHeight() - (backgroundHeight - textTopMargin);

                    drawText(textX, textY, canvas);
                }
            }

        } else {
            backgroundLeft = 10;
            canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10, canvas.getHeight() - 10, dpToPx(radius), dpToPx(radius), rectPaint);

            float textX = backgroundLeft + 10;
            float textY = canvas.getHeight() - (backgroundHeight - textTopMargin);

            drawText(textX, textY, canvas);
        }

    }

    private void drawText(float textX, float textY, Canvas canvas) {
        elementsList.clear();
        Paint textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSize);


        if (addresses != null) {

            place = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
            road = addresses.get(0).getAddressLine(0);

            elementsList.add(place);
            elementsList.add(road);

            if (showLatLng) {
                latlng = "Lat Lng : " + latitude + ", " + longitude;
                elementsList.add(latlng);
            }

        }



        if (showDate) {
            date = new SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(new Date());
            elementsList.add(date);
        }
        if (showAuthorName) {
            authorName = "Clicked by : " + authorName;
            elementsList.add(authorName);
        }
        for (String item : elementsList) {
            canvas.drawText(item, textX, textY, textPaint);
            textY += textTopMargin;
        }

        if (showAppName) {
            String appName = GTIUtility.getApplicationName(context);
            if (imageQuality != null) {
                switch (imageQuality) {
                    case STANDARD -> {
                        textSize = (float) (textSize) / 2;
                        textTopMargin = 50 * 2;
                        textY = canvas.getHeight() - 20;
                        textPaint.setTextSize(textSize);
                        canvas.drawText(appName, (canvas.getWidth() - 10) - 10 - textPaint.measureText(appName), textY, textPaint);
                    }
                    case HIGH -> {
                        textSize = (float) (textSize) / 2;
                        textTopMargin = (float) (50 * 3.6);
                        textPaint.setTextSize(textSize);
                        textY = canvas.getHeight() - 40;
                        canvas.drawText(appName, (canvas.getWidth() - 10) - 20 - textPaint.measureText(appName), textY, textPaint);
                    }
                }
            } else {
                textTopMargin = 50;
                textPaint.setTextSize(textSize / 2);
                textY = canvas.getHeight() - 20;
                canvas.drawText(appName, (canvas.getWidth() - 10) - 10 - textPaint.measureText(appName), textY, textPaint);

            }
        }
        
    }

    private float dpToPx(float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }


    private void storeBitmapInternally(Bitmap b) {
        File pictureFile = getOutputMediaFile();
        returnFile = pictureFile;
        if (pictureFile == null) {
            Log.e("ASHISH", "Error creating media file, check storage permissions: ");
            return;
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] compressedImageData = outputStream.toByteArray();
            FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
            fileOutputStream.write(compressedImageData);
            fileOutputStream.close();
            Log.d("ASHISH", "file compressed " + Arrays.toString(compressedImageData));
        } catch (IOException e) {
            Log.e("ASHISH", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
        }

    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        String mImageName = "GTI" + timeStamp + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File mediaFile = new File(imagePath);
        MediaScannerConnection.scanFile(context,
                new String[]{imagePath}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });

        return mediaFile;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public void setCustomFont(Typeface typeface) {
        this.typeface = typeface;
    }

    public void setBackgroundRadius(float radius) {
        this.radius = radius;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void showAuthorName(boolean showAuthorName) {
        this.showAuthorName = showAuthorName;
    }

    public void showAppName(boolean showAppName) {
        this.showAppName = showAppName;
    }

    public void showLatLng(boolean showLatLng) {
        this.showLatLng = showLatLng;
    }

    public void showDate(boolean showDate) {
        this.showDate = showDate;
    }

    public void showGoogleMap(boolean showGoogleMap) {
        this.showGoogleMap = showGoogleMap;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setImageQuality(String imageQuality) {
        this.imageQuality = imageQuality;

        switch (imageQuality) {
            case STANDARD -> {
                bitmapWidth = 960 * 2;
                bitmapHeight = 1280 * 2;
                backgroundHeight = (float) (backgroundHeight * 2);
                textSize = (float) (textSize * 2);
                textTopMargin = 50 * 2;
                radius = radius * 2;
            }
            case HIGH -> {
                bitmapWidth = (int) (960 * 3.6);
                bitmapHeight = (int) (1280 * 3.6);
                backgroundHeight = (float) (backgroundHeight * 3.6);
                textSize = (float) (textSize * 3.6);
                textTopMargin = (float) (50 * 3.6);
                radius = (float) (radius * 3.6);
            }
        }

    }

    public String getImagePath() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String mImageName = "GTI" + timeStamp + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File media = new File(imagePath);
        MediaScannerConnection.scanFile(context,
                new String[]{media.getAbsolutePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });

        return imagePath;
    }

    public Uri getImageUri() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String mImageName = "GTI" + timeStamp + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File media = new File(imagePath);
        return Uri.fromFile(media);
    }

    public void handlePermissionGrantResult() {
        permissionCallback.onPermissionGranted();
    }

}
