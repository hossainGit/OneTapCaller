package com.example.elder_phone.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    // Create a file URI for camera capture
    fun createImageFileUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    // Compress and resize image to avoid OOM errors
    suspend fun compressAndResizeImage(
        context: Context,
        originalUri: Uri,
        maxWidth: Int = 720,
        maxHeight: Int = 720,
        quality: Int = 80
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(originalUri)
            inputStream?.use { stream ->
                // First, decode with bounds only to get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)

                // Calculate sampling ratio
                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                options.inJustDecodeBounds = false

                // Decode with sampling
                val resizedBitmap = BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(originalUri),
                    null,
                    options
                )

                // Rotate if needed (for camera images)
                val rotatedBitmap =
                    resizedBitmap?.let { rotateImageIfRequired(context, it, originalUri) }

                // Compress and save
                rotatedBitmap?.let { saveBitmapToFile(context, it, quality) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Calculate sampling ratio for efficient loading
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (width, height) = options.run { outWidth to outHeight }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    // Rotate image based on EXIF orientation
    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    else -> return bitmap
                }

                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    // Save bitmap to file and return URI
    private fun saveBitmapToFile(context: Context, bitmap: Bitmap, quality: Int): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                saveBitmapToMediaStore(context, bitmap, quality)
            } else {
                // Use file system for older versions
                saveBitmapToFileSystem(context, bitmap, quality)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Save to MediaStore (Android 10+)
    private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, quality: Int): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "contact_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }
        }

        return uri
    }

    // Save to file system (Pre-Android 10)
    private fun saveBitmapToFileSystem(context: Context, bitmap: Bitmap, quality: Int): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, "contact_${timeStamp}.jpg")

        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    // Convert bitmap to byte array (for database storage if needed)
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    // Get image dimensions without loading full bitmap
    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                Pair(options.outWidth, options.outHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Check if image file size is within reasonable limits
    suspend fun isImageSizeReasonable(context: Context, uri: Uri, maxSizeMB: Double = 5.0): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val sizeBytes = inputStream?.available()?.toLong() ?: 0
                val sizeMB = sizeBytes / (1024.0 * 1024.0)
                sizeMB <= maxSizeMB
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Create circular bitmap (alternative to ShapeableImageView if needed)
    fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }
        val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }
}