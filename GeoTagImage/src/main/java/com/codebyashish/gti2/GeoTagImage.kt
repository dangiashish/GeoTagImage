/*
 * MIT License
 *
 * Copyright (c) 2025 Ashish Dangi
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
package com.codebyashish.gti2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.codebyashish.geotagimage.GTIUtility.getApplicationName
import com.codebyashish.geotagimage.GTIUtility.getMapKey
import com.codebyashish.geotagimage.GTIUtility.isGoogleMapsLinked
import com.codebyashish.geotagimage.ImageQuality.AVERAGE
import com.codebyashish.geotagimage.ImageQuality.HIGH
import com.codebyashish.geotagimage.ImageQuality.LOW
import com.codebyashish.geotagimage.LocationUtil
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class GeoTagImage(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>
) {
    private var place = ""
    private var road = ""
    private var latlng = ""
    private var date = ""
    private var mapBitmap: Bitmap? = null
    private var addresses: List<Address>? = null
    private var IMAGE_EXTENSION = ".png"
    private var fileUri: Uri? = null
    private var geocoder: Geocoder? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var customTextSize = 25f
    private var textTopMargin = 0f
    private var typeface = Typeface.DEFAULT
    private var radius = dpToPx(6f)
    private var backgroundColor = "#66000000".toColorInt()
    private var textColor = Color.WHITE
    private var backgroundHeight = 150f
    private var authorName: String = ""
    private var label: String = "Clicked By"
    private var showAuthorName = false
    private var showAppName = false
    private var showLatLng = true
    private var showDate = true
    private var showGoogleMap = true
    private val elementsList = ArrayList<String>()
    private var mapHeight = backgroundHeight.toInt()
    private var mapWidth = 120
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var apiKey: String? = null
    private var center: String? = null
    private var dimension: String? = null
    private var markerUrl: String? = null
    private var imageQuality: String? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private val TAG = Companion::class.java.simpleName
    private var isActive = true
    private var currentPhotoPath: String? = null
    private var latestLocation: Location? = null

    fun preparePhotoUriAndLocation(onReady: (Uri?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fetchCurrentLocation {
                fileUri = createImageInternally()?.let {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                }
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
                addresses = geocoder!!.getFromLocation(latitude, longitude, 1)
                if (isGoogleMapsLinked(context)) {
                    if (getMapKey(context) == null) {
                        processCapturedImage()
                    }
                    apiKey = getMapKey(context)
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
                        15,
                        dimension,
                        markerUrl,
                        "satellite",
                        apiKey
                    )
                    executorService.submit(LoadImageTask(imageUrl))

                } else if (!isGoogleMapsLinked(context)) {
                    processCapturedImage()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("error ", "${e.message}")
            }
        }

    }

    private inner class LoadImageTask(private val imageUrl: String) : Runnable {
        override fun run() {
            try {
                val bitmap = loadImageFromUrl(imageUrl)
                if (bitmap != null) {
                    mapBitmap = bitmap
//                    val newBitmap = createBitmap()
//                    storeBitmapInternally(newBitmap)
                    processCapturedImage()
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
    fun processCapturedImage(geoTagged: Boolean = isActive): Uri? {
        currentPhotoPath?.let { filePath ->
            val file = File(filePath)
            val bitmap = BitmapFactory.decodeFile(filePath)

            val resizedBitmap = bitmap.scale(768, 1024)

            if (!geoTagged) {
                val outputStream = file.outputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.close()
                return Uri.fromFile(file)
            }

            val geoTaggedBitmap = drawTextOnBitmap(resizedBitmap)

            val outputStream = file.outputStream()
            geoTaggedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()

            return Uri.fromFile(file)
        }
        return null
    }

    private fun drawTextOnBitmap(bitmap: Bitmap): Bitmap {
        elementsList.clear()
        if (addresses != null) {
            place = addresses!![0].locality + ", " + addresses!![0].adminArea + ", " + addresses!![0].countryName
            road = addresses!![0].getAddressLine(0)
            elementsList.add(place)
            elementsList.add(road)
            Log.d(TAG, "drawText: imgQuality $imageQuality")
            if (showLatLng) {
                latlng = "Lat Lng : $latitude, $longitude"
                elementsList.add(latlng)
            }
        }
        if (showDate) {
            date = SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(Date())
            elementsList.add(date)
        }
        if (showAuthorName) {
            elementsList.add("$label : $authorName")
        }

        if (showAppName){
            elementsList.add(getApplicationName(context))
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

        if (!showGoogleMap){
            mapWidth = 0
        }

        val maxTextWidth = result.width - mapWidth - 60

        mapBitmap?.let {
            val scaledMap = it.scale(mapWidth, mapHeight, false)
            canvas.drawBitmap(scaledMap, 10f, canvas.height - 220f, design)
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

        val left = mapWidth + 10f
        val top = result.height - blockHeight - 30f
        val right = left + blockWidth
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

    private fun createImageInternally(): File? {
        return try {
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File =
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            File.createTempFile("IMG_${timeStamp}_", IMAGE_EXTENSION, storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: IOException) {
            Toast.makeText(
                context,
                "Error creating file: ${ex.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
            null
        }
    }

    private fun fetchCurrentLocation(onLocationReady: () -> Unit) {
        try {
            LocationUtil.fetchLocation(context) { location ->
                latestLocation = location
                deviceLocation(location)
                onLocationReady()

            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
            onLocationReady()
        }
    }

    fun setTextSize(textSize: Float) {
        this.customTextSize = textSize
    }

    fun setCustomFont(typeface: Typeface?) {
        this.typeface = typeface
    }

    fun setBackgroundRadius(radius: Float) {
        this.radius = radius
    }

    fun setBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
    }

    fun setTextColor(textColor: Int) {
        this.textColor = textColor
    }

    fun showAuthorName(showAuthorName: Boolean) {
        this.showAuthorName = showAuthorName
    }

    fun showAppName(showAppName: Boolean) {
        this.showAppName = showAppName
    }

    fun showLatLng(showLatLng: Boolean) {
        this.showLatLng = showLatLng
    }

    fun showDate(showDate: Boolean) {
        this.showDate = showDate
    }

    fun showGoogleMap(showGoogleMap: Boolean) {
        this.showGoogleMap = showGoogleMap
    }

    fun setAuthorName(authorName: String) {
        this.authorName = authorName
    }

    fun setLabel(label: String) {
        this.label = label
    }

    @Deprecated("This function is deprecated, standard picture size is 768x1024")
    fun setImageQuality(imageQuality: String?) {
        this.imageQuality = imageQuality
        when (imageQuality) {
            LOW -> {
                customTextSize = 20f
                bitmapWidth = (960 / 1.5).toInt()
                bitmapHeight = (1280 / 1.5).toInt()
                textTopMargin = (50f / 1.5).toFloat()
                backgroundHeight = (150f / 1.5).toFloat()
                mapWidth = 120
                mapHeight = (backgroundHeight.toInt() / 1.5).toInt()
            }

            AVERAGE -> {
                bitmapWidth = 960
                bitmapHeight = 1280
                textTopMargin = 50f
                backgroundHeight = 150f
                mapWidth = 120
                mapHeight = backgroundHeight.toInt()
            }

            HIGH -> {
                bitmapWidth = (960 * 3.6).toInt()
                bitmapHeight = (1280 * 3.6).toInt()
                backgroundHeight = (backgroundHeight * 2)
                customTextSize = (customTextSize * 3.6).toFloat()
                textTopMargin = (50 * 3.6).toFloat()
                radius = (radius * 3.6).toFloat()
                mapWidth = (mapWidth * 2)
                mapHeight = (backgroundHeight.toInt() * 1.5).toInt()
            }
        }
    }

    fun setImageExtension(imgExtension: String) {
        when (imgExtension) {
            JPG -> IMAGE_EXTENSION = ".jpg"
            PNG -> IMAGE_EXTENSION = ".png"
            JPEG -> IMAGE_EXTENSION = ".jpeg"
        }
    }

    fun enableGTIService(isActive: Boolean) {
        this.isActive = isActive
    }

    companion object {
        const val PNG = ".png"
        const val JPG = ".jpg"
        const val JPEG = ".jpeg"
    }

    fun requestCameraAndLocationPermissions() {
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
        val mImageName = "IMG_$timeStamp$IMAGE_EXTENSION"
        Log.d(TAG, "imagePath: $IMAGE_EXTENSION")
        val ImagePath = mediaStorageDir.path + File.separator + mImageName
        val media = File(ImagePath)
        MediaScannerConnection.scanFile(context, arrayOf(media.absolutePath), null) { path, uri -> }
        return ImagePath
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
        val mImageName = "IMG_$timeStamp$IMAGE_EXTENSION"
        val imagePath = mediaStorageDir.path + File.separator + mImageName
        val media = File(imagePath)
        return Uri.fromFile(media)
    }
}
