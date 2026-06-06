package com.roadsaathi.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun saveBitmap(bitmap: Bitmap): String {
        val dir = File(context.filesDir, "hazard_photos")
        if (!dir.exists()) dir.mkdirs()
        val fileName = "hazard_${UUID.randomUUID()}.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
        return file.absolutePath
    }

    fun saveBitmapWithExif(bitmap: Bitmap, latitude: Double, longitude: Double): String {
        val path = saveBitmap(bitmap)
        return try {
            injectExifGps(path, latitude, longitude)
            path
        } catch (e: Exception) {
            Timber.w(e, "Failed to inject EXIF GPS data")
            path
        }
    }

    private fun injectExifGps(path: String, latitude: Double, longitude: Double) {
        val exif = ExifInterface(path)
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, toDms(latitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, toDms(longitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")
        exif.saveAttributes()
    }

    private fun toDms(coordinate: Double): String {
        val abs = Math.abs(coordinate)
        val degrees = abs.toInt()
        val minutesFull = (abs - degrees) * 60
        val minutes = minutesFull.toInt()
        val seconds = (minutesFull - minutes) * 60
        return "$degrees/1,$minutes/1,${"%.0f".format(seconds * 1000)}/1000"
    }

    fun getFile(path: String): File = File(path)

    fun deleteImage(path: String): Boolean = File(path).delete()

    fun getImageUri(path: String): Uri = Uri.fromFile(File(path))
}
