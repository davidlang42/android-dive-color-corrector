package com.davidlang.divecolorcorrector

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.lifecycle.ViewModel
import java.io.FileDescriptor
import java.io.IOException

class ImageViewModel : ViewModel() {
    private var _image: Bitmap? = null
    val image: Bitmap?
        get() = _image

    fun load(uri: Uri, context: Activity) {
        val bitmap = getBitmapFromUri(uri, context)
        if (bitmap == null) {
            Toast.makeText(context, "Image uri could not be loaded", Toast.LENGTH_LONG).show()
            return
        }
        _image = bitmap
    }

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri, context: Activity): Bitmap? {
        val parcelFileDescriptor: ParcelFileDescriptor =
            context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }
}