<p align="center">
<img src="https://github.com/dangiashish/GeoTagImage/assets/70362030/88c3e47a-0029-4d90-8276-540558137ccc"/>
</p>

<div align = "center">
<h1 align="center"> 💫 GeoTagImage (GTI) 📸🌍 </h1>
<a href="https://www.codefactor.io/repository/github/dangiashish/geotagimage/overview/master"><img src="https://www.codefactor.io/repository/github/dangiashish/geotagimage/badge/master" alt="CodeFactor" /></a>
<a href="https://jitpack.io/#dangiashish/GeoTagImage"><img src="https://jitpack.io/v/dangiashish/GeoTagImage.svg"/></a>
<a href="(https://developer.android.com/tools/sdkmanager"><img src="https://img.shields.io/badge/android--sdk-24%2B-green"/></a>
<a href="https://www.java.com/"><img src="https://img.shields.io/badge/compatible-java-blue"/></a>
<a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/compatible-kotlin-blueviolet"/></a>

#### Read the documentation on [ashishdangi.medium.com](https://ashishdangi.medium.com/geotags-on-images-in-android-studio-334753c0489f) 

<br/>

<a href="https://github.com/dangiashish/GeoTagImage/suites/18290122215/artifacts/1057687744"><img src="https://upload.wikimedia.org/wikipedia/commons/1/11/Download_apk.png" width="300px" height="80"/></a>

</div>

### Gradle

Add repository in your `settings.gradle`
 
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
#### OR 
in your `settings.gradle.kts`
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven( url = "https://jitpack.io")
    }
}

```
### Add dependency :

Add dependency in your `build.gradle` (module-level) file :

```groovy
dependencies{

    implementation 'com.github.dangiashish:GeoTagImage:1.1.0'
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

#### XML : 
 Go to here --> [activity_main.xml](https://github.com/dangiashish/GeoTagImage/blob/master/app/src/main/res/layout/activity_main.xml)

#### Java : [MainActivity.java](https://github.com/dangiashish/GeoTagImage/blob/afad2aca53837da4de3c37163911ed897bc3c540/app/src/main/java/com/codebyashish/geotagimage/MainActivity.java)
```groovy
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
```
#### handle onActivityResult method
```groovy
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
                        geoTagImage.setBackgroundColor(Color.parseColor("#66000000"));
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
#### preview the original image
```groovy
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

        
