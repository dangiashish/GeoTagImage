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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.codebyashish.geotagimage.GTIPermissions.checkCameraLocationPermission
import com.codebyashish.geotagimage.GTIUtility.getApplicationName
import com.codebyashish.geotagimage.GTIUtility.getMapKey
import com.codebyashish.geotagimage.GTIUtility.isGoogleMapsLinked
import com.codebyashish.geotagimage.ImageQuality.AVERAGE
import com.codebyashish.geotagimage.ImageQuality.HIGH
import com.codebyashish.geotagimage.ImageQuality.LOW
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.time.times

class GeoTagImage(private val context: Context, callback: PermissionCallback) {
    private var place = ""
    private var road = ""
    private var latlng = ""
    private var date = ""
    private var originalImageHeight = 0
    private var originalImageWidth = 0
    private var returnFile: File? = null
    private var bitmap: Bitmap? = null
    private var mapBitmap: Bitmap? = null
    private var addresses: List<Address>? = null
   private var IMAGE_EXTENSION = ".png"
    private var fileUri: Uri? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var geocoder: Geocoder? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var textSize = 0f
    private var textTopMargin = 0f
    private var typeface: Typeface? = null
    private var radius = 0f
    private var backgroundColor = 0
    private var textColor = 0
    private var backgroundHeight = 0f
    private var backgroundLeft = 0f
    private var authorName: String = ""
    private var showAuthorName = false
    private var showAppName = true
    private var showLatLng = true
    private var showDate = true
    private var showGoogleMap = true
    private val elementsList = ArrayList<String>()
    private var mapHeight = 0
    private var mapWidth = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var apiKey: String? = null
    private var center: String? = null
    private var imageUrl: String? = null
    private var dimension: String? = null
    private var markerUrl: String? = null
    private var imageQuality: String? = null
    private val permissionCallback: PermissionCallback = callback
    private val executorService = Executors.newSingleThreadExecutor()
    private val TAG = Companion::class.java.simpleName
    private var isActive = true

    fun createImage(fileUri: Uri?) {
        if (fileUri == null) {
            throw GTIException("Uri cannot be null")
        }
        this.fileUri = fileUri

        // set default values here.
        textSize = 25f
        typeface = Typeface.DEFAULT
        radius = dpToPx(6f)
        backgroundColor = Color.parseColor("#66000000")
        textColor = context.getColor(android.R.color.white)
        backgroundHeight = 150f
//        authorName = ""
//        showAuthorName = false
//        showAppName = false
//        showGoogleMap = true
//        showLatLng = true
//        showDate = true
        mapHeight = backgroundHeight.toInt()
        mapWidth = 120
//        imageQuality = null
        if (isActive) {
            initialization()
        } else {
            bypassImage()
        }
    }

    private fun bypassImage() {
        if (imageQuality == null) {
            bitmapWidth = 960 * 2
            bitmapHeight = 1280 * 2
            backgroundHeight = (backgroundHeight * 2)
            mapWidth = 120 * 2
            mapHeight = backgroundHeight.toInt()
            textSize *= 2
            textTopMargin = (50 * 2).toFloat()
            radius *= 2
        }
        val bitmap = createBitmap()
        storeBitmapInternally(bitmap)
    }

    private fun initialization() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        if (imageQuality == null) {
            bitmapWidth = 960 * 2
            bitmapHeight = 1280 * 2
            backgroundHeight = (backgroundHeight * 2)
            mapWidth = 120 * 2
            mapHeight = backgroundHeight.toInt()
            textSize *= 2
            textTopMargin = (50 * 2).toFloat()
            radius *= 2
        }
        deviceLocation()
        //        getDimensions();
    }

    private fun deviceLocation() {
        if (checkCameraLocationPermission(context)) {
            val task = fusedLocationProviderClient!!.lastLocation
            task.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    geocoder = Geocoder(context, Locale.getDefault())
                    try {
                        addresses = geocoder!!.getFromLocation(latitude, longitude, 1)
                        if (isGoogleMapsLinked(context)) {
                            if (getMapKey(context) == null) {
                                val bitmap = createBitmap()
                                storeBitmapInternally(bitmap)
                                throw GTIException("API key not found for this project")
                            } else {
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
                                imageUrl = String.format(
                                    Locale.getDefault(),
                                    "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%d&size=%s&%s&maptype=%s&key=%s",
                                    center,
                                    15,
                                    dimension,
                                    markerUrl,
                                    "satellite",
                                    apiKey
                                )
                                executorService.submit(LoadImageTask(imageUrl!!))
                            }
                        } else if (!isGoogleMapsLinked(context)) {
                            val bitmap = createBitmap()
                            storeBitmapInternally(bitmap)
                            throw GTIException("Project is not linked with google map sdk")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("error ", "${e.message}")
                    }
                }
            }
        }
    }

    private inner class LoadImageTask(private val imageUrl: String) : Runnable {
        override fun run() {
            try {
                val bitmap = loadImageFromUrl(imageUrl)
                if (bitmap != null) {
                    mapBitmap = bitmap
                    val newBitmap = createBitmap()
                    storeBitmapInternally(newBitmap)
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

    fun shutdown() {
        executorService.shutdown()
    }

    fun dimension() {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        try {
            BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(fileUri!!),
                null,
                options
            )
            originalImageHeight = options.outHeight
            originalImageWidth = options.outWidth
            Log.d(TAG, "$originalImageHeight & $originalImageWidth")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            throw GTIException("File Not Found : " + e.message)

        }
    }

    private fun createBitmap(): Bitmap {
        val b = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        canvas.drawARGB(0, 255, 255, 255)
        canvas.drawRGB(255, 255, 255)
        copyTheImage(canvas)
        return b
    }

    private fun copyTheImage(canvas: Canvas) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri!!)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val design = Paint()
        val scaledbmp = Bitmap.createScaledBitmap(bitmap!!, bitmapWidth, bitmapHeight, false)
        canvas.drawBitmap(scaledbmp, 0f, 0f, design)
        val rectPaint = Paint()
        rectPaint.color = backgroundColor
        rectPaint.style = Paint.Style.FILL
        if (showAuthorName) {
            backgroundHeight += textTopMargin
        }
        if (showDate) {
            backgroundHeight += textTopMargin
        }
        Log.d(TAG, "copyTheImage: showAppName $showAppName")
        if (showLatLng) {
            backgroundHeight += textTopMargin
        }
        if (isActive) {
            if (isGoogleMapsLinked(context)) {
                if (mapBitmap != null) {
                    if (showGoogleMap) {
                        val mapLeft = 10f
                        backgroundLeft = (mapBitmap!!.width + 20).toFloat()
                        canvas.drawRoundRect(
                            backgroundLeft,
                            canvas.height - backgroundHeight,
                            (canvas.width - 10).toFloat(),
                            (canvas.height - 10).toFloat(),
                            dpToPx(radius),
                            dpToPx(radius),
                            rectPaint
                        )
                        val scaledbmp2 = Bitmap.createScaledBitmap(
                            mapBitmap!!, mapWidth, mapHeight, false
                        )
                        canvas.drawBitmap(
                            scaledbmp2,
                            mapLeft,
                            canvas.height - backgroundHeight + (backgroundHeight - mapBitmap!!.height) / 2,
                            design
                        )
                        val textX = backgroundLeft + 10
                        val textY = canvas.height - (backgroundHeight - textTopMargin)
                        drawText(textX, textY, canvas)
                    } else {
                        backgroundLeft = 10f
                        canvas.drawRoundRect(
                            backgroundLeft,
                            canvas.height - backgroundHeight,
                            (canvas.width - 10).toFloat(),
                            (canvas.height - 10).toFloat(),
                            dpToPx(radius),
                            dpToPx(radius),
                            rectPaint
                        )
                        val textX = backgroundLeft + 10
                        val textY = canvas.height - (backgroundHeight - textTopMargin)
                        drawText(textX, textY, canvas)
                    }
                }
            } else {
                backgroundLeft = 10f
                canvas.drawRoundRect(
                    backgroundLeft,
                    canvas.height - backgroundHeight,
                    (canvas.width - 10).toFloat(),
                    (canvas.height - 10).toFloat(),
                    dpToPx(radius),
                    dpToPx(radius),
                    rectPaint
                )
                val textX = backgroundLeft + 10
                val textY = canvas.height - (backgroundHeight - textTopMargin)
                drawText(textX, textY, canvas)
            }
        }
    }

    private fun drawText(textX: Float, textYDir: Float, canvas: Canvas) {
        var textY = textYDir
        if (imageQuality == null) {
            textSize *= 2
            textTopMargin = (50 * 2).toFloat()
        }
        elementsList.clear()
        val textPaint = Paint()
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.setTypeface(typeface)
        textPaint.textSize = textSize
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
            elementsList.add("Clicked by : $authorName")
        }
        for (item in elementsList) {
            canvas.drawText(item, textX, textY, textPaint)
            textY += textTopMargin
        }
        if (showAppName) {
            val appName = getApplicationName(context)
            if (imageQuality != null) {
                when (imageQuality) {
                    LOW -> {
                        textTopMargin = 50f
                        textPaint.textSize = textSize / 3
                        textY = (canvas.height - 20).toFloat()
                        canvas.drawText(
                            appName,
                            canvas.width - 10 - 10 - textPaint.measureText(appName),
                            textY,
                            textPaint
                        )
                    }

                    AVERAGE -> {
                        textTopMargin = 50f
                        textPaint.textSize = textSize / 2
                        textY = (canvas.height - 20).toFloat()
                        canvas.drawText(
                            appName,
                            canvas.width - 10 - 10 - textPaint.measureText(appName),
                            textY,
                            textPaint
                        )
                    }

                    HIGH -> {
                        textSize /= 2
                        textTopMargin = (50 * 3.6).toFloat()
                        textPaint.textSize = textSize
                        textY = (canvas.height - 40).toFloat()
                        canvas.drawText(
                            appName,
                            canvas.width - 10 - 20 - textPaint.measureText(appName),
                            textY,
                            textPaint
                        )
                    }
                }
            } else {
                textSize /= 2
                textTopMargin = (50 * 2).toFloat()
                textY = (canvas.height - 20).toFloat()
                textPaint.textSize = textSize
                canvas.drawText(
                    appName,
                    canvas.width - 10 - 10 - textPaint.measureText(appName),
                    textY,
                    textPaint
                )
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun storeBitmapInternally(b: Bitmap) {
        val pictureFile = outputMediaFile()
        returnFile = pictureFile
        if (pictureFile == null) {
            Log.e(TAG, "Error creating media file, check storage permissions: ")
            return
        }
        try {
            val outputStream = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val compressedImageData = outputStream.toByteArray()
            val fileOutputStream = FileOutputStream(pictureFile)
            fileOutputStream.write(compressedImageData)
            fileOutputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "${e.message}")
            e.printStackTrace()
        }
    }

    private fun outputMediaFile(): File? {
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
        val mediaFile = File(imagePath)
        MediaScannerConnection.scanFile(
            context, arrayOf(imagePath), null
        ) { path, uri -> }
        return mediaFile
    }

    fun setTextSize(textSize: Float) {
        this.textSize = textSize
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

    fun setImageQuality(imageQuality: String?) {
        this.imageQuality = imageQuality
        when (imageQuality) {
            LOW -> {
                textSize = 20f
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
                backgroundHeight = (backgroundHeight * 2).toFloat()
                textSize = (textSize * 3.6).toFloat()
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

    fun getImagePath(): String? {
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
        Log.d(TAG, "imagePath: $IMAGE_EXTENSION")
        val ImagePath = mediaStorageDir.path + File.separator + mImageName
        val media = File(ImagePath)
        MediaScannerConnection.scanFile(context, arrayOf(media.absolutePath), null) { path, uri -> }
        return ImagePath
    }

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

    fun handlePermissionGrantResult() {
        permissionCallback.onPermissionGranted()
    }

    fun enableGTIService(isActive: Boolean) {
        this.isActive = isActive
    }

    companion object {
        const val PNG = ".png"
        const val JPG = ".jpg"
        const val JPEG = ".jpeg"
    }
}
