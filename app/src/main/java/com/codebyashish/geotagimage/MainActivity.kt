/*
 * MIT License
 *
 * Copyright (c) 2023 Ashish Dangi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.codebyashish.geotagimage

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.codebyashish.geotagimage.GTIPermissions.checkCameraLocationPermission
import com.codebyashish.geotagimage.GTIPermissions.requestCameraLocationPermission
import com.codebyashish.geotagimage.GTIUtility.generateOriginalFile
import com.codebyashish.geotagimage.GTIUtility.getFileUri
import com.codebyashish.geotagimage.GTIUtility.optimizeBitmap
import com.codebyashish.geotagimage.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity(), PermissionCallback {
    private var fileUri: Uri? = null
    private lateinit var geoTagImage: GeoTagImage
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        binding.btnGithub.setOnClickListener {
            val url = "https://github.com/dangiashish/GeoTagImage"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(Uri.parse(url))
            startActivity(intent)
        }


        // initialize the context
        mContext = this@MainActivity

        // initialize the GeoTagImage class object with context and callback
        // use try/catch block to handle exceptions.
        geoTagImage = GeoTagImage(mContext as MainActivity, this)



        // setOnClickListener on camera button.
        binding.ivCamera.setOnClickListener {
            // first check permission for camera and location by using GTIPermission class.
            if (checkCameraLocationPermission(mContext)) {

                // if permissions are granted, than open camera.
                openCamera()
            } else {
                // otherwise request for the permissions by using GTIPermission class.
                requestCameraLocationPermission(mContext, PERMISSION_REQUEST_CODE)
            }
        }
    }

    // if permissions are granted for camera and location.
    private fun openCamera() {
        // call Intent for ACTION_IMAGE_CAPTURE which will redirect to device camera.
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // create a file object

        // before adding GeoTags, generate or create an original image file
        // We need to create an original image to add geotags by copying this file.
        val file: File? = generateOriginalFile(mContext, GeoTagImage.PNG)
        if (file != null) {
            // if file has been created, then will catch its path for future reference.
            originalImgStoragePath = file.path
        }

        // now get Uri from this created image file by using GTIUtility.getFileUri() function.
        fileUri = getFileUri(mContext, file)

        // pass this uri file into intent filters while opening camera.
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)

        // call ActivityResultLauncher by passing the intent request.
        activityResultLauncher.launch(intent)
    }

    private var activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            // Handle the result here
            binding.progressBar.visibility = View.VISIBLE
            binding.ivCamera.visibility = View.GONE

            // TODO : START THE MAIN FUNCTIONALITY

            // now call the function createImage() and pass the uri object (line no. 100-110)
            geoTagImage.createImage(fileUri)

            // set all the customizations for geotagging as per your requirements.
            geoTagImage.setTextSize(30f)
            geoTagImage.setBackgroundRadius(5f)
            geoTagImage.setBackgroundColor(Color.parseColor("#66000000"))
            geoTagImage.setTextColor(Color.WHITE)
            geoTagImage.setAuthorName("Ashish")
            geoTagImage.showAuthorName(true)
            geoTagImage.showAppName(true)
            geoTagImage.setImageQuality(ImageQuality.HIGH)
            geoTagImage.setImageExtension(GeoTagImage.PNG)
            geoTagImage.enableGTIService(true)  // Enable/Disable GTI Features.

            // after geotagged photo is created, get the new image path by using getImagePath() method
            gtiImageStoragePath = geoTagImage.imagePath().toString()

            Log.i("GTIPath", "$gtiImageStoragePath: ")

            binding.tvOriginalPath.text = originalImgStoragePath

            /* The time it takes for a Canvas to draw items on a blank Bitmap can vary depending on several factors,
                     * such as the complexity of the items being drawn, the size of the Bitmap, and the processing power of the device.*/
            Handler(Looper.getMainLooper()).postDelayed(
                { previewCapturedImage() }, 3000
            )
        }
    }

    // preview of the original image
    private fun previewCapturedImage() {
        try {
            val bitmap = optimizeBitmap(gtiImageStoragePath)
            binding.ivImage.setImageBitmap(bitmap)
            if (binding.ivImage.drawable != null) {
                binding.ivClose.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
            binding.ivClose.setOnClickListener { v: View? ->
                binding.ivImage.setImageBitmap(null)
                binding.ivCamera.visibility = View.VISIBLE
                binding.ivClose.visibility = View.GONE
                binding.ivImage.setImageDrawable(null)
                binding.tvGTIPath.text = ""
                binding.tvOriginalPath.text = ""
            }
            binding.tvGTIPath.text = gtiImageStoragePath
            binding.tvOriginalPath.text = originalImgStoragePath
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GeoTagImage", "previewCapturedImage: ${e.message}", )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                geoTagImage.handlePermissionGrantResult()
                Toast.makeText(mContext, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPermissionGranted() {
        openCamera()
    }

    override fun onPermissionDenied() {
        requestCameraLocationPermission(mContext, PERMISSION_REQUEST_CODE)
    }

    companion object {
        private var originalImgStoragePath: String? = null
        private var gtiImageStoragePath: String = ""
        private const val PERMISSION_REQUEST_CODE = 100
        lateinit var mContext: FragmentActivity
    }
}
