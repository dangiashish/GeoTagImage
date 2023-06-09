package com.codebyashish.geotagimage;

import android.content.Context;
import android.content.pm.PackageManager;
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
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

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

public class GeoTagImage {
    private String place = "", road = "", latlng = "", date = "";
    private int originalImageHeight = 0;
    private int originalImageWidth = 0;
    private Context context;
    private Bitmap bitmap = null, mapBitmap = null;
    private List<Address> addresses;
    private static final String IMAGE_EXTENSION = "png";
    private Uri fileUri;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;
    private double latitude, longitude;
    private float textSize;
    private Typeface typeface;
    private float radius;
    private int backgroundColor, textColor;
    private float backgroundHeight;
    private String authorName;
    private boolean showAuthorName, showLatLng, showDate, showGoogleMap;
    private ArrayList<String> elementsList = new ArrayList<>();
    private int mapHeight, mapWidth;
    private String apiKey, center, imageUrl, dimension, markerUrl, imageStoragePath;


    public GeoTagImage(Context context) {
        this.context = context;


    }

    public void createImage(Uri fileUri) {
        this.fileUri = fileUri;

        // set default values here.
        textSize = 25f;
        typeface = Typeface.DEFAULT;
        radius = dpToPx(6);
        backgroundColor = Color.parseColor("#66000000");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textColor = context.getColor(android.R.color.white);
        }
        backgroundHeight = 150f;
        authorName = "";
        showAuthorName = false;
        showGoogleMap = true;
        showLatLng = true;
        showDate = true;
        mapHeight = (int) backgroundHeight;
        mapWidth = 120;

        initialization();
    }

    private void initialization() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        getDeviceLocation();
        getDimensions();


    }

    private void getDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> task = fusedLocationProviderClient.getLastLocation();
            task.addOnSuccessListener(location -> {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    geocoder = new Geocoder(context, Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);

                        if (Util.isGoogleMapsLinked(context)) {
                            apiKey = Util.getMapKey(context);

                            center = latitude + "," + longitude;
                            dimension = mapWidth + "x" + mapHeight;
                            markerUrl = String.format(Locale.getDefault(), "%s%s%s", "markers=color:red%7C", center, "&");
                            imageUrl = String.format(Locale.getDefault(), "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%d&size=%s&%s&maptype=%s&key=%s",
                                    center, 15, dimension, markerUrl, "satellite", apiKey);

                            new LoadMapImageTask().execute(imageUrl);
                        } else {
                            Bitmap bitmap = createImage();
                            storeBitmapInternally(bitmap);
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
                Bitmap bitmap = createImage();
                storeBitmapInternally(bitmap);
            }
        }
    }

    public void getDimensions() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(fileUri), null, options);
            originalImageHeight = options.outHeight;
            originalImageWidth = options.outWidth;
            Log.d("ASHISH", originalImageHeight + " & " + originalImageWidth);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d("ASHISH", "file not Found" + e.getMessage());
        }
    }

    private Bitmap createImage() {
        Bitmap b = Bitmap.createBitmap(originalImageWidth/4, originalImageHeight/4, Bitmap.Config.ARGB_8888);
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
        Bitmap scaledbmp = Bitmap.createScaledBitmap(bitmap, originalImageWidth/4, originalImageHeight/4, false);
        canvas.drawBitmap(scaledbmp, 0, 0, design);

        Log.i("xoxo" , " dimen "+ originalImageWidth + "x" + originalImageHeight);

        Paint rectPaint = new Paint();
        rectPaint.setColor(backgroundColor);
        rectPaint.setStyle(Paint.Style.FILL);

        if (showAuthorName) {
            backgroundHeight = backgroundHeight + 50;
        }
        if (showDate) {
            backgroundHeight = backgroundHeight + 50;
        }
        if (showLatLng) {
            backgroundHeight = backgroundHeight + 50;
        }

        if (Util.isGoogleMapsLinked(context)) {
            if (mapBitmap != null) {
                if (showGoogleMap) {
                    float mapLeft = 10;
                    float backgroundLeft = mapBitmap.getWidth() + 20;
                    canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10, canvas.getHeight() - 10, dpToPx(radius), dpToPx(radius), rectPaint);
                    Bitmap scaledbmp2 = Bitmap.createScaledBitmap(mapBitmap, mapWidth, mapHeight, false);
                    canvas.drawBitmap(scaledbmp2, mapLeft, (canvas.getHeight() - backgroundHeight) + (backgroundHeight - mapBitmap.getHeight()) / 2, design);

                    float textX = backgroundLeft + 10;
                    float textY = canvas.getHeight() - (backgroundHeight - 50);

                    drawText(textX, textY, canvas);
                } else {
                    float backgroundLeft = 10;
                    canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10, canvas.getHeight() - 10, dpToPx(radius), dpToPx(radius), rectPaint);

                    float textX = backgroundLeft + 10;
                    float textY = canvas.getHeight() - (backgroundHeight - 50);

                    drawText(textX, textY, canvas);
                }
            }

        } else {
            float backgroundLeft = 10;
            canvas.drawRoundRect(backgroundLeft, canvas.getHeight() - backgroundHeight, canvas.getWidth() - 10, canvas.getHeight() - 10, dpToPx(radius), dpToPx(radius), rectPaint);

            float textX = backgroundLeft + 10;
            float textY = canvas.getHeight() - (backgroundHeight - 50);

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

            if (showDate) {
                date = new SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(new Date());
                elementsList.add(date);
            }

            if (showAuthorName) {
                authorName = "Clicked by : " + authorName;
                elementsList.add(authorName);
            }

        }

        for (String item : elementsList) {
            canvas.drawText(item, textX, textY, textPaint);
            textY += 50;
        }

    }

    private float dpToPx(float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }


    private void storeBitmapInternally(Bitmap b) {
        File pictureFile = getOutputMediaFile();
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());

        String mImageName = "IMG" + timeStamp + "." + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        imageStoragePath = imagePath;
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

    public void setAuthorNameEnabled(boolean authorNameEnabled) {
        this.showAuthorName = authorNameEnabled;
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

    public String getImagePath() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String mImageName = "IMG" + timeStamp + "." + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File media = new File(imagePath);
        MediaScannerConnection.scanFile(context,
                new String[]{media.getAbsolutePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ASHISH", "check image in gallery");
                    }
                });
        Log.i("ASHISH", "" + imagePath);
        return imagePath;
    }

    public Uri getImageUri() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String mImageName = "IMG" + timeStamp + "." + IMAGE_EXTENSION;
        String imagePath = mediaStorageDir.getPath() + File.separator + mImageName;
        File media = new File(imagePath);

        MediaScannerConnection.scanFile(context,
                new String[]{media.getAbsolutePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ASHISH", "Check image in gallery: " + uri.toString());
                    }
                });
        return Uri.fromFile(media);
    }

}
