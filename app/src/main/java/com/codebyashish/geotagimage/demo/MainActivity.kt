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

package com.codebyashish.geotagimage.demo

import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.codebyashish.geotagimage.ImageQuality
import com.codebyashish.gti.PermissionCallback
import com.codebyashish.geotagimage.demo.databinding.ActivityMainBinding
import com.dangiashish.GeoTagImage
import java.io.File
import java.text.DecimalFormat

class MainActivity : AppCompatActivity(), PermissionCallback {
    private var gtiUri: Uri? = null
    private lateinit var geoTagImage: GeoTagImage
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private val TAG = "GeoTagImageLog"
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnGithub.setOnClickListener {
            val url = "https://github.com/dangiashish/GeoTagImage"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = url.toUri()
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback {
            clearAppData(this@MainActivity)
            finish()
        }

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

        // initialize the GeoTagImage class object with context and callback
        geoTagImage = GeoTagImage(this, permissionLauncher)
        geoTagImage.requestCameraAndLocationPermissions()
        geoTagImage.enableCameraX(false)

        // initialize the context
        mContext = this@MainActivity


        // setOnClickListener on camera button.
        binding.ivCamera.setOnClickListener {
            geoTagImage.launchCamera { uri ->
                if (uri != null) {
                    gtiUri = uri
                    Log.d(TAG, "onCreate: $gtiUri")
                    Log.d(TAG, "onCreate: ${gtiUri?.path!!}")
                    previewCapturedImage()
                } else {
                    Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.sFeatures.setOnCheckedChangeListener { _, isChecked ->
            geoTagImage.enableGTIService(isChecked)  // Enable/Disable GTI Features.
            if (!isChecked) {
                geoTagImage.showAuthorName(false)
                binding.etAuthorName.visibility = View.GONE
                binding.sAuthor.isChecked = false
                geoTagImage.showAppName(false)
                binding.sApp.isChecked = false
                geoTagImage.showLatLng(false)
                binding.sLatLng.isChecked = false
                geoTagImage.showDate(false)
                binding.sDate.isChecked = false
                geoTagImage.showGoogleMap(false)
                binding.sMap.isChecked = false

            }
        }

        binding.sAuthor.setOnCheckedChangeListener { _, isChecked ->
            geoTagImage.showAuthorName(isChecked)
            binding.etAuthorName.visibility = if (isChecked) View.VISIBLE else View.GONE

        }

        binding.etAuthorName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                geoTagImage.setAuthorName(s.toString().trim())
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.sApp.setOnCheckedChangeListener { _, isChecked ->
            geoTagImage.showAppName(isChecked)
        }

        binding.sLatLng.setOnCheckedChangeListener { _, isChecked ->
            geoTagImage.showLatLng(isChecked)
        }

        binding.sDate.setOnCheckedChangeListener { _, isChecked ->
            geoTagImage.showDate(isChecked)
        }

        binding.sMap.setOnCheckedChangeListener { _, isChecked ->
            geoTagImage.showGoogleMap(isChecked)
        }

        binding.toggleAppearanceRandom.check(R.id.button_ext_jpeg)
        binding.toggleAppearanceRandom.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_ext_png -> {
                        geoTagImage.setImageExtension(GeoTagImage.Companion.PNG)
                    }

                    R.id.button_ext_jpeg -> {
                        geoTagImage.setImageExtension(GeoTagImage.Companion.JPEG)
                    }
                }
            }
        }

        binding.toggleCamera.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.toggle_camera_x -> {
                        geoTagImage.enableCameraX(true)
                    }

                    R.id.toggle_system_camera -> {
                        geoTagImage.enableCameraX(false)
                    }
                }
            }
        }

        binding.toggleImageQuality.check(R.id.button_img_low)
        if (binding.toggleImageQuality.checkedButtonId == R.id.button_img_low) {
            imageQuality = ImageQuality.LOW
        }
        binding.toggleImageQuality.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_img_low -> {
                        imageQuality = ImageQuality.LOW
                    }

                    R.id.button_img_average -> {
                        imageQuality = ImageQuality.AVERAGE
                    }

                    R.id.button_img_high -> {
                        imageQuality = ImageQuality.HIGH
                    }
                }
            }
        }

    }

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
                    binding.ivClose.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE

                    binding.tvGTIPath.text = gtiUri?.path
                    binding.tvImgSize.text = getFileSize(gtiUri?.path!!)


                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image: ${e.message}")
                }
            }
            binding.ivClose.setOnClickListener { v: View? ->
                binding.ivImage.setImageBitmap(null)
                binding.ivCamera.visibility = View.VISIBLE
                binding.ivClose.visibility = View.GONE
                binding.ivImage.setImageDrawable(null)
                binding.tvGTIPath.text = ""
                binding.tvImgSize.text = ""
            }
        }
    }

    private fun viewInGallery(gtiImageStoragePath: String) {
        val file = File(gtiImageStoragePath)
        if (file.exists()) {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    mContext,
                    "${applicationContext.packageName}.provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } else {
            Log.e(TAG, "viewInGallery: file not exist")
        }

    }

    override fun onPermissionGranted() {

    }

    override fun onPermissionDenied() {
        geoTagImage.requestCameraAndLocationPermissions()
    }

    companion object {
        lateinit var mContext: FragmentActivity
        private var imageQuality = ""
    }

    private fun getFileSize(filePath: String?): String {
        val file = filePath?.let { File(it) }

        if (file!!.exists()) {
            val fileSizeInBytes = file.length()
            val fileSizeInKB = fileSizeInBytes / 1024.0
            val fileSizeInMB = fileSizeInKB / 1024.0

            val decimalFormat = DecimalFormat("#.##")

            return when {
                fileSizeInMB >= 1 -> "~ ${decimalFormat.format(fileSizeInMB)} MB"
                fileSizeInKB >= 1 -> "~ ${decimalFormat.format(fileSizeInKB)} KB"
                else -> "~ $fileSizeInBytes Bytes" // Return size in Bytes
            }
        } else {
            return ""
        }
    }

    fun clearAppData(context: Context) {
        try {
            val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            var success = true
            picturesDir?.listFiles()?.forEach { file ->
                if (file.isFile) {
                    success = success && file.delete()
                }
            }
            Toast.makeText(context, "Deleted $success", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}