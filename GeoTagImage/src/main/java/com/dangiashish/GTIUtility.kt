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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.security.cert.Extension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GTIUtility {
    private val TAG = "GTIUtility"

    /** Check for Map SDK Integration */
    @JvmStatic
    fun isGoogleMapsLinked(context: Context): Boolean {
        val packageManager = context.packageManager
        var apiKey: String? = null
        try {
            val applicationInfo =
                packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val metaData = applicationInfo.metaData
            if (metaData != null && metaData.containsKey("com.google.android.geo.API_KEY")) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return apiKey != null
    }

    /** Check for google map api key (if Map SDK is integrated) */
    @JvmStatic
    fun getMapKey(context: Context): String? {
        val packageManager = context.packageManager
        var apiKey: String? = null
        try {
            val applicationInfo =
                packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val metaData = applicationInfo.metaData
            if (metaData != null && metaData.containsKey("com.google.android.geo.API_KEY")) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return apiKey
    }

    /** To get application name */
    @JvmStatic
    fun getApplicationName(context: Context): String {
        val packageManager = context.packageManager
        val applicationInfo: ApplicationInfo? = try {
            packageManager.getApplicationInfo(context.applicationInfo.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return (if (applicationInfo != null) packageManager.getApplicationLabel(applicationInfo) else "Unknown") as String
    }

    @Deprecated("")
    /** Save original image in camera directory  */
    @JvmStatic
    fun generateOriginalFile(mContext: FragmentActivity, imageExtension: String): File? {
        var file: File? = null
        Log.i(TAG, "generateOriginalFile: Step 1")
        try {
            val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera"
            )
            Log.w(TAG, "generateOriginalFile: Step 2 : $mediaStorageDir", )
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null
                }
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            file =
                File(mediaStorageDir.path + File.separator + "IMG_" + timeStamp + imageExtension)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (file != null) {
            scanMediaFIle(mContext, file)
        }
        Log.w(TAG, "generateOriginalFile: Step 3 $file", )
        return file
    }

    private fun scanMediaFIle(mContext: FragmentActivity, file: File) {
        MediaScannerConnection.scanFile(
            mContext, arrayOf(file.absolutePath),
            null
        ) { _: String?, _: Uri? -> }
    }

    /** Optimize bitmap to prevent OutOfMemory Exception */
    @JvmStatic
    fun optimizeBitmap(filePath: String?): Bitmap {
        val options = BitmapFactory.Options()
        options.inSampleSize = 4
        return BitmapFactory.decodeFile(filePath, options)
    }

    /** get File Uri from application provider */
    @JvmStatic
    fun getFileUri(context: Context, file: File?): Uri {
        return FileProvider.getUriForFile(context, context.packageName + ".provider", file!!)
    }
}