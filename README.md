# GeoTagImage


[![](https://jitpack.io/v/dangiashish/GeoTagImage.svg)](https://jitpack.io/#dangiashish/GeoTagImage)
[![](https://img.shields.io/badge/android--sdk-24%2B-green)](https://developer.android.com/tools/sdkmanager)
[![](https://img.shields.io/badge/compatible-java-blue)](https://www.java.com/)
[![](https://img.shields.io/badge/compatible-kotlin-blueviolet)](https://kotlinlang.org/)

[](https://miro.medium.com/v2/resize:fit:720/format:webp/1*rcAxNUUVIi4yZ_sBI_vTRw.jpeg)

#### Read the documentation on [ashishdangi.medium.com](https://ashishdangi.medium.com/geotags-on-images-in-android-studio-334753c0489f)

### Gradle

Add repository in your `build.gradle` (project-level) file :
```gradle
allprojects {
      repositories {
	...
	maven { url 'https://jitpack.io' }
	}
}
```
##### OR 
in your `settings.gradle`
 
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```
### Add dependency :

Add dependency in your `build.gradle` (module-level) file :

```groovy
dependencies{

    implementation 'com.github.dangiashish:GeoTagImage:1.0.7'
}
```
#### Add file provider in [AndroidManifest.xml](https://github.com/dangiashish/GeoTagImage/blob/afad2aca53837da4de3c37163911ed897bc3c540/app/src/main/AndroidManifest.xml#L34)
```groovy
<provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/provider_paths" />
        </provider>

```
#### Create an xml file for path provider [@xml/provider_path.xml](https://github.com/dangiashish/GeoTagImage/blob/afad2aca53837da4de3c37163911ed897bc3c540/app/src/main/res/xml/provider_paths.xml)
```groovy
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="external_files" path="."/>
</paths>
```
#### Java : [MainActivity.java](https://github.com/dangiashish/GeoTagImage/blob/afad2aca53837da4de3c37163911ed897bc3c540/app/src/main/java/com/codebyashish/geotagimage/MainActivity.java)
```groovy
public class MainActivity extends AppCompatActivity implements PermissionCallback {
    private ImageView ivCamera, ivImage, ivClose;
    private static String imageStoragePath;
    public static final String IMAGE_EXTENSION = ".jpg";
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
}
```
#### openCamera()
```groovy
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
```
#### handle request permisson result
```groovy
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
 ```
#### handle onActivityResult method
```groovy
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
                    geoTagImage.setTextSize(25f);
                    geoTagImage.setBackgroundRadius(5f);
                    geoTagImage.setBackgroundColor(Color.parseColor("#66000000"));
                    geoTagImage.setTextColor(getColor(android.R.color.white));
                    geoTagImage.setAuthorName("Ashish");
                    geoTagImage.showAuthorName(true);
                    geoTagImage.showAppName(true);

                    // after the geotagged photo is created, get the new image path by using getImagePath() method
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
```
#### priview the original image
```groovy
        // preview of the original image
    private void previewCapturedImage() {
        try {
            ivCamera.setVisibility(View.GONE);
            Bitmap bitmap = GTIUtility.optimizeBitmap(imageStoragePath);
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
    public void onPermissionGranted() {
        openCamera();
    }

    @Override
    public void onPermissionDenied() {
        GTIPermissions.requestCameraLocationPermission(mContext, PERMISSION_REQUEST_CODE);
    }
```
    
#### LICENSE
```
MIT License

Copyright (c) 2023 Ashish Dangi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

        
