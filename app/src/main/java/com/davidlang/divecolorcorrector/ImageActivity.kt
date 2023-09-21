package com.davidlang.divecolorcorrector

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.davidlang.divecolorcorrector.ui.theme.DiveColorCorrectorTheme
import java.io.File
import java.io.FileDescriptor
import java.io.IOException


class ImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != Intent.ACTION_SEND) {
            Toast.makeText(this, "Expected send action", Toast.LENGTH_SHORT).show()
            return
        }
        if (intent.type?.startsWith("image/") != true) {
            Toast.makeText(this, "Expected image MIME type", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        if (uri == null) {
            Toast.makeText(this, "Intent did not specify image uri", Toast.LENGTH_SHORT).show()
            return
        }
        setContent {
            MainContent(uri)
        }
    }

    @Composable
    fun MainContent(uri: Uri) {
        var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var exifData by remember { mutableStateOf<Map<String, String>?>(null) }
        var filter by remember { mutableStateOf<ColorMatrix?>(null) }
        var progress by remember { mutableStateOf(0f) }
        DiveColorCorrectorTheme {
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (originalBitmap != null && filter != null) {
                        ImageContent(originalBitmap!!.asImageBitmap(), ColorFilter.colorMatrix(filter!!))
                    } else {
                        LoadingContent(progress)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { finish() },
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text("Cancel", fontSize = 20.sp)
                    }
                    if (renderedBitmap != null) { //TODO  && exifData != null
                        Button(
                            onClick = {
                                saveBitmap(renderedBitmap!!, uri) //TODO exifData!!
                                finish()
                            },
                            modifier = Modifier.padding(10.dp),
                        ) {
                            Text("Save a copy", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        Thread {
            val parcel = contentResolver.openFileDescriptor(uri, "r")
            if (parcel != null) {
                originalBitmap = BitmapFactory.decodeFileDescriptor(parcel.fileDescriptor)
                progress = 0.1f
                exifData = readExifData(parcel.fileDescriptor)
                parcel.close()
                progress = 0.2f
                val corrector = ColorCorrector(originalBitmap!!)
                filter = corrector.underwaterFilter()//TODO progress during analysis
                progress = 0.8f
                renderedBitmap = corrector.applyFilter(filter!!)//TODO progress during rendering
                progress = 1f
            }
        }.start()
    }

    @Composable
    fun ImageContent(image: ImageBitmap, filter: ColorFilter) {
        Image(
            bitmap = image,
            contentDescription = "Color corrected image",
            colorFilter = filter
        )
    }

    @Composable
    fun LoadingContent(progress: Float) {
        Box(contentAlignment = Alignment.Center) {
            LinearProgressIndicator(progress)
        }
    }

    private fun readExifData(fileDescriptor: FileDescriptor): Map<String, String> {
        val data = mutableMapOf<String, String>()
        val exifReader = ExifInterface(fileDescriptor)
        val tagsToCopy: Array<String> = arrayOf(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_MAKE) //TODO static
        for (tag in tagsToCopy) {
            val value = exifReader.getAttribute(tag)
            if (value != null) {
                data[tag] = value
            }
        }
        return data.toMap()
    }

    private fun saveBitmap(bitmap: Bitmap, originalUri: Uri) {
        val originalName = removeExtension(sanitizeFileName(getFileName(originalUri)))
        val filePath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/DiveColorCorrector"
        val dir = File(filePath)
        if (!dir.exists())
            dir.mkdirs()
        var file = File(dir, "${originalName}_corrected.jpg")
        var i = 1
        while (file.exists()) {
            file = File(dir, "${originalName}_corrected ($i).jpg")
            i += 1
        }
        val fOut = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fOut)
        fOut.flush()
        fOut.close()
        Toast.makeText(this, "Saved as ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun removeExtension(fileName: String): String {
        val parts = fileName.split(".")
        return if (parts.size < 2)
            fileName
        else
            parts.slice(0 until (parts.size - 1)).joinToString(".")
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            return cursor.getString(nameIndex)
        } else {
            return uri.lastPathSegment ?: "unknown"
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }
}