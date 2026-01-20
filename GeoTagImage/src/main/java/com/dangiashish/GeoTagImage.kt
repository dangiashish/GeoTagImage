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

package com.dangiashish

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Window
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.dangiashish.geotagimage.R
import com.dangiashish.geotagimage.databinding.CameraLayoutBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @param context FragmentActivity or AppCompatActivity
 * @param permissionLauncher ActivityResultLauncher for camera and location permissions
 * @param cameraLauncher ActivityResultLauncher for camera capture (mandatory, if app/developer required using system camera)
 */
class GeoTagImage(
    private val context: FragmentActivity,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val cameraLauncher: ActivityResultLauncher<Uri>? = null
) {
    private var address: String = ""
    private var latLong = ""
    private var date = ""
    private var mapBitmap: Bitmap? = null
    private var imageExtension = ".jpg"
    private var fileUri: Uri? = null
    private var geocoder: Geocoder? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var customTextSize = 25f
    private var typeface = Typeface.DEFAULT
    private var radius = dpToPx(6f)
    private var backgroundColor = "#66000000".toColorInt()
    private var textColor = Color.WHITE
    private var backgroundHeight = 150f
    private var authorName: String = ""
    private var label: String = "Captured By"
    private var exifAppName: String? = null
    private var showAuthorName = false
    private var showAppName = false
    private var showLatLng = true
    private var showDate = true
    private var showGoogleMap = true
    private val elementsList = ArrayList<String>()
    private var mapHeight = backgroundHeight.toInt()
    private var mapWidth = 140
    private var apiKey: String? = null
    private var center: String? = null
    private var dimension: String? = null
    private var markerUrl: String? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private val TAG = "GTILogs"
    private var isEnabled = true
    private var currentPhotoPath: String? = null
    private var latestLocation: Location? = null

    // CameraX related variables
    private var useCameraX = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Camera UI components (managed internally)
    private var cameraDialog: Dialog? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var pendingCallback: ((Uri?) -> Unit)? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var currentZoomRatio = 1f
    private var cameraAspectRatio: Int = AspectRatio.RATIO_4_3
    private lateinit var binding: CameraLayoutBinding
    enum class FlashMode { ON, OFF, AUTO }
    private var flashMode = FlashMode.OFF
    private var flashCode = 0


    /**
     * Launch camera interface
     * If CameraX is enabled, shows custom camera UI
     * If CameraX is disabled, uses system camera (original behavior)
     * @param onImageCaptured callback with captured image URI
     */
    fun launchCamera(onImageCaptured: (Uri?) -> Unit, onFailure: (String?) -> Unit) {
        if (useCameraX) {
            val lifecycleOwner = getLifecycleOwner()
            if (lifecycleOwner != null) {
                showCameraXInterface(onImageCaptured)
            } else {
                Log.e(TAG, "Context must be FragmentActivity or AppCompatActivity for CameraX")
                onImageCaptured(null)
                onFailure.invoke("Context must be FragmentActivity or AppCompatActivity for CameraX")
            }
        } else {
            if (cameraLauncher == null) {
                onFailure.invoke("CameraLauncher is not initialized")
                return
            }
            pendingCallback = onImageCaptured
            preparePhotoUriAndLocation(onImageCaptured)
        }
    }

    private fun getLifecycleOwner(): LifecycleOwner? {
        return when (context) {
            else -> context
        }
    }

    private fun showCameraXInterface(onImageCaptured: (Uri?) -> Unit) {
        if (!checkCameraPermissions()) {
            requestCameraAndLocationPermissions()
            onImageCaptured(null)
            return
        }

        createCameraDialog(onImageCaptured)
        startCameraX()
    }

    private fun createCameraDialog(onImageCaptured: (Uri?) -> Unit) {
        var mediaPlayer: MediaPlayer? = null

        cameraDialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            binding = CameraLayoutBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.zoomSeekBar.max = 90
            binding.zoomSeekBar.progress = 0
            binding.zoomValue.text = "1.0x"

            binding.previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

            scaleGestureDetector = ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return false

                        val minZoom = zoomState.minZoomRatio
                        val maxZoom = zoomState.maxZoomRatio

                        currentZoomRatio *= detector.scaleFactor
                        currentZoomRatio = currentZoomRatio.coerceIn(minZoom, maxZoom)
                        camera?.cameraControl?.setZoomRatio(currentZoomRatio)

                        val uiZoom = 1f + ((currentZoomRatio - minZoom) / (maxZoom - minZoom)) * 9f
                        binding.zoomSeekBar.progress = ((uiZoom - 1f) * 10).toInt()
                        binding.zoomValue.text = String.format(Locale.US, "%.1fx", uiZoom)

                        return true
                    }
                })

            binding.previewView.setOnTouchListener { v, event ->
                scaleGestureDetector?.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) v.performClick()
                true
            }

            binding.zoomSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val zoomState = camera?.cameraInfo?.zoomState?.value ?: return

                    val minZoom = zoomState.minZoomRatio
                    val maxZoom = zoomState.maxZoomRatio

                    // UI zoom 1x to 10x
                    val uiZoom = 1f + (progress / 10f)

                    val cameraZoom = minZoom + ((uiZoom - 1f) / 9f) * (maxZoom - minZoom)

                    camera?.cameraControl?.setZoomRatio(cameraZoom)
                    currentZoomRatio = cameraZoom

                    binding.zoomValue.text = String.format(Locale.US, "%.1fx", uiZoom)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            binding.btnCapture.setOnClickListener {
                if (mediaPlayer == null) mediaPlayer =
                    MediaPlayer.create(context, R.raw.sound_shutter)
                mediaPlayer?.start()

                it.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

                capturePhotoWithCameraX(onImageCaptured)
            }

            binding.btnClose.setOnClickListener { closeCameraDialog() }
            binding.btnFlip.setOnClickListener { flipCamera() }

            binding.ivFlash.setOnClickListener {
                if (flashCode >= 2) flashCode = 0 else flashCode += 1
                updateFlashUI()

            }


            show()
        }
    }

    private fun flipCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Restart camera with new selector
        startCameraX()
    }

    private fun closeCameraDialog() {
        cameraDialog?.dismiss()
        cameraDialog = null
        stopCameraX()
    }

    private fun startCameraX() {
        val lifecycleOwner = getLifecycleOwner() ?: return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val screenMetrics = context.resources.displayMetrics
        val screenAspectRatio = aspectRatio(screenMetrics.widthPixels, screenMetrics.heightPixels)

        // Adjust PreviewView Aspect Ratio
        binding.previewView.post {
            binding.previewView.let { pv ->
                val layoutParams = pv.layoutParams as ConstraintLayout.LayoutParams
                when (cameraAspectRatio) {
                    RATIO_4X3 -> {
                        layoutParams.dimensionRatio = "H,3:4"
                        layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                        layoutParams.height = 0
                    }

                    RATIO_16X9 -> {
                        layoutParams.dimensionRatio = "H,9:16"
                        layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                        layoutParams.height = 0
                    }

                    RATIO_FULL -> {
                        layoutParams.dimensionRatio = null
                        layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                        layoutParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                    }

                    RATIO_1X1 -> {
                        layoutParams.dimensionRatio = "H,1:1"
                        layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
                        layoutParams.height = 0
                    }
                }
                pv.layoutParams = layoutParams
            }
        }

        // Preview
        val previewBuilder = Preview.Builder()

        // ImageCapture
        val imageCaptureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(
                when (flashMode) {
                    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                    FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                }
            )

        if (cameraAspectRatio == 3) { // 1:1 Special case
            val size = Size(1080, 1080)
            previewBuilder.setTargetResolution(size)
            imageCaptureBuilder.setTargetResolution(size)
        } else {
            val ratio = if (cameraAspectRatio == 2) screenAspectRatio else cameraAspectRatio
            previewBuilder.setTargetAspectRatio(ratio)
            imageCaptureBuilder.setTargetAspectRatio(ratio)
        }

        val preview = previewBuilder.build().also {
            it.surfaceProvider = binding.previewView.surfaceProvider
        }

        imageCapture = imageCaptureBuilder.build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture
            )

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun updateFlashUI() {
        when (flashCode) {
            0 -> {
                camera?.cameraControl?.enableTorch(false)
                flashMode = FlashMode.OFF
                binding.ivFlash.setImageResource(R.drawable.baseline_flash_off_24)
            }

            1 -> {
                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    camera?.cameraControl?.enableTorch(true)
                    flashMode = FlashMode.ON
                    binding.ivFlash.setImageResource(R.drawable.baseline_flash_on_24)
                }
            }

            2 -> {
                camera?.cameraControl?.enableTorch(false)
                flashMode = FlashMode.AUTO
                binding.ivFlash.setImageResource(R.drawable.baseline_flash_auto_24)
            }
        }
    }

    private fun capturePhotoWithCameraX(onImageCaptured: (Uri?) -> Unit) {
        val imageCapture = imageCapture ?: run {
            onImageCaptured(null)
            return
        }

        // Get current location first
        fetchCurrentLocation {
            // Create output file
            val photoFile = createImageInternally()
            if (photoFile == null) {
                onImageCaptured(null)
                return@fetchCurrentLocation
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        (context as? Activity)?.runOnUiThread {
                            onImageCaptured(null)
                            closeCameraDialog()
                        }
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        currentPhotoPath = photoFile.absolutePath

                        // Process the captured image
                        val processedUri = processCapturedImage()

                        (context as? Activity)?.runOnUiThread {
                            onImageCaptured(processedUri)
                            closeCameraDialog()
                        }
                    }
                }
            )
        }
    }

    private fun stopCameraX() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun checkCameraPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun preparePhotoUriAndLocation(onReady: (Uri?) -> Unit) {
        if (useCameraX) {
            Log.w(TAG, "CameraX is enabled. Use launchCamera() instead.")
            onReady(null)
            return
        }

        if (checkCameraPermissions()) {
            fetchCurrentLocation {
                fileUri = createImageInternally()?.let {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                }
                cameraLauncher?.launch(fileUri!!)
                onReady(fileUri)
            }
        } else {
            onReady(null)
            requestCameraAndLocationPermissions()
        }
    }

    private fun deviceLocation(location: Location?) {
        if (location != null) {
            latitude = location.latitude
            longitude = location.longitude
            geocoder = Geocoder(context, Locale.getDefault())
            try {
                address = geocoder!!.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    ?.getAddressLine(0)
                    ?: "Location: Not available"
                if (GTIUtility.isGoogleMapsLinked(context)) {
                    apiKey = GTIUtility.getMapKey(context)
                    center = "$latitude,$longitude"
                    dimension = mapWidth.toString() + "x" + mapHeight
                    markerUrl = String.format(
                        Locale.getDefault(),
                        "%s%s%s",
                        "markers=color:red%7C",
                        center,
                        "&"
                    )
                    val imageUrl = String.format(
                        Locale.getDefault(),
                        "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%d&size=%s&%s&maptype=%s&key=%s",
                        center,
                        17,
                        dimension,
                        markerUrl,
                        "satellite",
                        apiKey
                    )
                    executorService.submit(LoadImageTask(imageUrl))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processBitmapAspectRatio(bitmap: Bitmap): Bitmap {
        return when (cameraAspectRatio) {
            3 -> { // 1:1
                val size = min(bitmap.width, bitmap.height)
                val x = (bitmap.width - size) / 2
                val y = (bitmap.height - size) / 2
                Bitmap.createBitmap(bitmap, x, y, size, size)
            }

            else -> bitmap
        }
    }

    private inner class LoadImageTask(private val imageUrl: String) : Runnable {
        override fun run() {
            try {
                val bitmap = loadImageFromUrl(imageUrl)
                if (bitmap != null) {
                    mapBitmap = bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadImageFromUrl(imageUrl: String): Bitmap? {
        try {
            val inputStream = URL(imageUrl).openStream()
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Call this after receiving image capture result.
     */
    fun processCapturedImage(geoTagged: Boolean = isEnabled): Uri? {
        currentPhotoPath?.let { filePath ->
            val file = File(filePath)

            var bitmap = BitmapFactory.decodeFile(filePath)

            bitmap = setOrientation(filePath, bitmap)

            bitmap = processBitmapAspectRatio(bitmap)

            val resizedBitmap = when (cameraAspectRatio) {
                RATIO_1X1 -> bitmap.scale(1024, 1024)
                RATIO_16X9 -> bitmap.scale(720, 1280)
                RATIO_FULL -> {
                    val maxSide = 1280
                    val width = bitmap.width
                    val height = bitmap.height

                    if (width >= height) {
                        val ratio = height.toFloat() / width
                        bitmap.scale(maxSide, (maxSide * ratio).toInt())
                    } else {
                        val ratio = width.toFloat() / height
                        bitmap.scale((maxSide * ratio).toInt(), maxSide)
                    }
                }

                else -> bitmap.scale(768, 1024)
            }

            if (!geoTagged) {
                saveImageToGallery(resizedBitmap)
                val outputStream = file.outputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.close()
                return Uri.fromFile(file)
            }

            val geoTaggedBitmap = drawTextOnBitmap(resizedBitmap)

            saveImageToGallery(geoTaggedBitmap)

            val outputStream = file.outputStream()
            geoTaggedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()

            latestLocation?.let { embedGeoTagInExif(filePath, it) }

            return Uri.fromFile(file)
        }
        return null
    }

    private fun embedGeoTagInExif(filePath: String, location: Location) {
        try {
            val exif = ExifInterface(filePath)

            val label = if (!exifAppName.isNullOrEmpty()) {
                ", Captured via $exifAppName"
            } else {
                ", Captured via GeoTagImage App"
            }
            exif.setAttribute(
                ExifInterface.TAG_DATETIME,
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
            exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL + label)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Android ${Build.VERSION.RELEASE}")

            exif.setGpsInfo(location)
            exif.saveAttributes()
        } catch (e: IOException) {
            Log.e(TAG, "Error writing EXIF data: ${e.localizedMessage}")
        }
    }

    /**
     * Set EXIF data for the captured image.
     */
    private fun setExif(exif: ExifInterface, location: Location) {
        val label = if (!exifAppName.isNullOrEmpty()) {
            ", Captured via $exifAppName"
        } else {
            ", Captured via GeoTagImage App"
        }
        exif.setAttribute(
            ExifInterface.TAG_DATETIME,
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
        exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL + label)
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Android ${Build.VERSION.RELEASE}")

        exif.setGpsInfo(location)
        exif.saveAttributes()
    }

    private fun saveImageToGallery(bitmap: Bitmap): Uri? {
        val resolver = context.contentResolver
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        var mimeType = "image/jpeg"
        when (imageExtension) {
            PNG -> mimeType = "image/png"
            JPEG -> mimeType = "image/jpeg"
        }


        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_$timeStamp$imageExtension")
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            latestLocation?.let { location ->
                uri?.let {
                    try {
                        context.contentResolver.openFileDescriptor(it, "rw")?.use { pfd ->
                            val exif = ExifInterface(pfd.fileDescriptor)
                            if (isEnabled) {
                                setExif(exif, location)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, ">Q Error writing EXIF via URI: ${e.localizedMessage}")
                    }
                }

            }
            uri
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + "/Camera"
            val file = File(imagesDir, "IMG_$timeStamp$imageExtension")

            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
            latestLocation?.let { location ->
                file.let {
                    try {
                        val exif = ExifInterface(file.absolutePath)
                        if (isEnabled) {
                            setExif(exif, location)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "<Q Error writing EXIF via URI: ${e.localizedMessage}")
                    }
                }
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    private fun setOrientation(path: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun drawTextOnBitmap(bitmap: Bitmap): Bitmap {
        elementsList.clear()
        elementsList.add(address)
        if (showLatLng) {
            latLong = "Lat Lng : $latitude, $longitude"
            elementsList.add(latLong)
        }
        if (showDate) {
            date = SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(Date())
            elementsList.add(date)
        }
        if (showAuthorName) {
            elementsList.add("$label $authorName")
        }

        if (showAppName) {
            elementsList.add("Captured via $exifAppName")
        }

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val textPaint = Paint().apply {
            color = textColor
            textSize = customTextSize
            isAntiAlias = true
            setShadowLayer(1f, 0f, 1f, Color.BLACK)
        }

        val bgPaint = Paint().apply {
            color = backgroundColor
        }

        val design = Paint()
        val padding = 20f
        val lineSpacing = 10f

        if (mapBitmap == null) {
            mapWidth = 0
        }

        if (!showGoogleMap) {
            mapWidth = 0
        }

        val maxTextWidth = result.width - mapWidth - 60

        mapBitmap?.let {
            if (mapWidth > 0 && mapHeight > 0) {
                val scaledMap = it.scale(mapWidth, mapHeight, false)
                canvas.drawBitmap(scaledMap, 10f, canvas.height - 160f, design)
            }
        }
        fun wrapText(line: String, paint: Paint, maxWidth: Int): List<String> {
            val words = line.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = testLine
                } else {
                    lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            return lines
        }

        val allWrappedLines = mutableListOf<String>()
        for (text in elementsList) {
            text.split("\n").forEach { line ->
                allWrappedLines.addAll(wrapText(line, textPaint, maxTextWidth))
            }
        }

        val textHeight = textPaint.fontMetrics.run { bottom - top }
        val blockHeight = allWrappedLines.size * (textHeight + lineSpacing)
        val blockWidth = maxTextWidth + padding * 2

        val left = mapWidth + 20f
        val top = result.height - blockHeight - 30f
        val right = left + blockWidth - 10f
        val bottom = top + blockHeight + padding

        canvas.drawRoundRect(RectF(left, top, right, bottom), 12f, 12f, bgPaint)

        var y = top + padding - textPaint.fontMetrics.top
        allWrappedLines.forEach { line ->
            canvas.drawText(line, left + padding, y, textPaint)
            y += textHeight + lineSpacing
        }

        return result
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    /**
     * Create a temporary image file
     * @return the file
     */
    private fun createImageInternally(): File? {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return null

        val fileName = "IMG_$timeStamp$imageExtension"
        return File(storageDir, fileName).apply {
            currentPhotoPath = absolutePath
        }
    }

    /**
     * Get current location
     * @param onLocationReady the callback to be called when location is ready
     */
    private fun fetchCurrentLocation(onLocationReady: () -> Unit) {
        try {
            GTILocationUtility.fetchLocation(context) { location ->
                if (location != null) {
                    latestLocation = location
                    deviceLocation(location)
                } else {
                    Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
                }
                onLocationReady()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            onLocationReady()
        }
    }

    /**
     * Set the text size
     * @param textSize the text size
     */
    fun setTextSize(textSize: Float) {
        this.customTextSize = textSize
    }

    /**
     * Set the custom font to be used
     * @param typeface the custom font to be used
     */
    fun setCustomFont(typeface: Typeface?) {
        this.typeface = typeface
    }

    /**
     * Set the background radius
     * @param radius the background radius
     */
    fun setBackgroundRadius(radius: Float) {
        this.radius = radius
    }

    /**
     * Set the background color
     * @param backgroundColor the background color
     */
    fun setBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
    }

    /**
     * Set the text color
     * @param textColor the text color
     */
    fun setTextColor(textColor: Int) {
        this.textColor = textColor
    }

    /**
     * Set whether to show the author name
     * @param showAuthorName true to show author name, false to hide author name
     */
    fun showAuthorName(showAuthorName: Boolean) {
        this.showAuthorName = showAuthorName
    }

    /**
     * Set whether to show the app name
     * @param showAppName true to show app name, false to hide app name
     */
    fun showAppName(showAppName: Boolean) {
        this.showAppName = showAppName
    }

    /**
     * Set whether to show the latitude and longitude
     * @param showLatLng true to show latitude and longitude, false to hide latitude and longitude
     */
    fun showLatLng(showLatLng: Boolean) {
        this.showLatLng = showLatLng
    }

    /**
     * Set whether to show the date
     * @param showDate true to show date, false to hide date
     */
    fun showDate(showDate: Boolean) {
        this.showDate = showDate
    }

    /**
     * Set whether to show Google Maps in the image
     * @param showGoogleMap true to show Google Maps, false to hide Google Maps
     */
    fun showGoogleMap(showGoogleMap: Boolean) {
        this.showGoogleMap = showGoogleMap
    }

    /**
     * Set the author name to be displayed in the image
     * @param authorName the author name to be displayed
     */
    fun setAuthorName(authorName: String) {
        this.authorName = authorName
    }

    /**
     * Set the label to be displayed in the image
     * @param label the label to be displayed
     */
    fun setLabel(label: String) {
        this.label = label
    }

    /**
     * Set the app name to be displayed in the image
     * @param appName the app name to be displayed
     */
    fun setAppName(appName: String) {
        this.exifAppName = appName
    }

    /**
     * Enable or disable CameraX usage
     * @param useCameraX true to use CameraX, false to use system camera
     */
    fun enableCameraX(useCameraX: Boolean) {
        this.useCameraX = useCameraX
    }

    /**
     * Set the aspect ratio for the camera preview and captured image
     * @param ratio Ratio_1X1, Ratio_4X3, Ratio_16X9, Ratio_Full
     */
    fun setCameraAspectRatio(ratio: Int) {
        this.cameraAspectRatio = ratio
        if (cameraProvider != null && cameraDialog?.isShowing == true) {
            startCameraX()
        }
    }

    /**
     * Set the image extension
     * @param imgExtension the image extension
     */
    fun setImageExtension(imgExtension: String) {
        when (imgExtension) {
            PNG -> imageExtension = ".png"
            JPEG -> imageExtension = ".jpg"
        }
    }

    /**
     * Enable or disable GeoTagService
     * @param isActive true to enable GeoTagService, false to disable GeoTagService
     */
    fun enableGTIService(isActive: Boolean) {
        this.isEnabled = isActive
    }

    /**
     * Clean up resources when done
     * Call this in onDestroy() of your Activity
     */
    fun cleanup() {
        closeCameraDialog()
        executorService.shutdown()
        cameraExecutor.shutdown()
    }

    companion object {
        const val PNG = ".png"
        const val JPEG = ".jpg"

        const val RATIO_1X1 = 3
        const val RATIO_4X3 = 0
        const val RATIO_16X9 = 1
        const val RATIO_FULL = 2
    }

    fun requestCameraAndLocationPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9 or below
            val permissionsToRequest = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
            return
        }
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    @Deprecated("getImageUri() is now deprecated, please use local {{URI?.path}}")
    fun getImagePath(): String {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "/"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return ""
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val mImageName = "IMG_$timeStamp$imageExtension"
        val imagePath = mediaStorageDir.path + File.separator + mImageName
        val media = File(imagePath)
        MediaScannerConnection.scanFile(context, arrayOf(media.absolutePath), null) { _, _ -> }
        return imagePath
    }

    @Deprecated("getImageUri() is now deprecated, please use local url preparePhotoUriAndLocation()")
    fun getImageUri(): Uri? {

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "/"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val mImageName = "IMG_$timeStamp$imageExtension"
        val imagePath = mediaStorageDir.path + File.separator + mImageName
        val media = File(imagePath)
        return Uri.fromFile(media)
    }
}