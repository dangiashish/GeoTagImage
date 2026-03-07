<p align="center">
<img src="https://github.com/dangiashish/GeoTagImage/assets/70362030/88c3e47a-0029-4d90-8276-540558137ccc"/>
</p>


<div align = "center">
<h1 align="center"> 💫 GeoTagImage (GTI) 📸🌍 </h1>
<a href="https://www.codefactor.io/repository/github/dangiashish/geotagimage/overview/master"><img src="https://www.codefactor.io/repository/github/dangiashish/geotagimage/badge/master" alt="CodeFactor" /></a>
<a href="https://jitpack.io/#dangiashish/GeoTagImage"><img src="https://jitpack.io/v/dangiashish/GeoTagImage.svg" alt=""/></a>
<a href="(https://developer.android.com/tools/sdkmanager"><img src="https://img.shields.io/badge/android--sdk-24%2B-green" alt=""/></a>
<a href="https://www.java.com/"><img src="https://img.shields.io/badge/compatible-java-blue" alt=""/></a>
<a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/compatible-kotlin-blueviolet" alt=""/></a>

#### Read the documentation on [ashishdangi.medium.com](https://ashishdangi.medium.com/geotags-on-images-in-android-studio-334753c0489f)

<br/>

<a href="https://github.com/dangiashish/GeoTagImage/actions/runs/18504814655/artifacts/4268116577"><img src="https://www.nthlink.com/assets/images/apk-download.png" width="300px" height="80"/></a>

</div>

```json lines
Key Features :

- Request camera & location permission callback by itself.
- CameraX api support.
- Show location and address.
- Show Lat Longs.
- Show author name.
- Show app name.
- Show Google map snapshot.
- Set Image Quality.
- Set Image Extension Type.
- Set Custom Typeface.
- Control the features of GTI.
- Set text size.
- Image File GeoTagging
- Camera Aspect Ratio Customization
- Camera Flash Customization
- Camera Zoom Customization
```

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

Updated

```gradle
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven( url = "https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven( url = "https://jitpack.io")
    }
}
```
### Add dependency :

Add dependency in your `build.gradle` (module-level) file :

```groovy
dependencies{

    implementation 'com.github.dangiashish:GeoTagImage:1.2.2'
}
```
#### OR
Add dependency in your `build.gradle.kts` (module-level) file :

```groovy
dependencies{

    implementation("com.github.dangiashish:GeoTagImage:1.2.2")
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

#### Kotlin : [MainActivity.kt](https://github.com/dangiashish/GeoTagImage/blob/afad2aca53837da4de3c37163911ed897bc3c540/app/src/main/java/com/codebyashish/geotagimage/MainActivity.kt)
```kotlin
import com.dangiashish.GeoTagImage
import com.dangiashish.PermissionCallback


class MainActivity : AppCompatActivity(), PermissionCallback{
    // create global variables
    private var gtiUri: Uri? = null
    private lateinit var geoTagImage: GeoTagImage
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ...
    }
}

```
### `onCreate()`
```kotlin
permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val allGranted = permissions.all { it.value }

    if (allGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

// cameraLauncher callback is required to capture the image from system camera.
cameraLauncher =
    registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            gtiUri = geoTagImage.processCapturedImage() // if captured via system camera
            previewCapturedImage()
        } else {
            Toast.makeText(mContext, "Image capture failed", Toast.LENGTH_SHORT).show()
        }
    }

// initialize the GeoTagImage class object with context and callback
// use try/catch block to handle exceptions.
geoTagImage = GeoTagImage(this, permissionLauncher, cameraLauncher) // this || context || requireContext() || requireActivity()
geoTagImage.requestCameraAndLocationPermissions()

```
#### openCamera()
```kotlin
     // setOnClickListener on camera button.
binding.ivCamera.setOnClickListener {
    geoTagImage.launchCamera(
            onImageCaptured = { uri ->  // if captured via CameraX api
                if (uri != null) {
                    gtiUri = uri
                    previewCapturedImage()
                } else {
                    Toast.makeText(mContext, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = {
                Toast.makeText(mContext, it, Toast.LENGTH_SHORT).show()
            }
    )
}
```
#### customize geo tags
```kotlin
     // set all the customizations for geotagging as per your requirements after initialization.

geoTagImage.enableGTIService(false) // Enable/Disable GTI Features

geoTagImage.setTextSize(30f)
geoTagImage.setBackgroundRadius(5f)
geoTagImage.setBackgroundColor(Color.parseColor("#66000000"))
geoTagImage.setTextColor(Color.WHITE)
geoTagImage.setAuthorName("Ashish")
geoTagImage.showAuthorName(true)
geoTagImage.showAppName(true)
geoTagImage.setImageQuality(ImageQuality.LOW) // Deprecated
geoTagImage.setImageExtension(PNG)
geoTagImage.setLabel("Clicked By") // Upload By || Author || Captured By
geoTagImage.setCameraAspectRatio(GeoTagImage.Ratio_4X3)

```

#### preview the original image
```kotlin
        // preview of the original image
private fun previewCapturedImage() {
    gtiUri?.let { uri ->
        binding.ivImage.let { imageView ->
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE

            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: ${e.message}")
            }
        }
    }
}
```

### Permission Callbacks
```kotlin
override fun onPermissionGranted() {

}

override fun onPermissionDenied() {
    geoTagImage.requestCameraAndLocationPermissions()
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

        
