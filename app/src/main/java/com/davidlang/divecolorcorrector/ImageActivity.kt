package com.davidlang.divecolorcorrector

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.Parcelable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        DiveColorCorrectorTheme {
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val image = bitmap?.asImageBitmap()
                    if (image == null) {
                        LoadingContent()
                    } else {
                        ImageContent(image)
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
                    if (bitmap != null) {
                        Button(
                            onClick = {
                                saveBitmap(bitmap!!, uri)
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
            val original = getBitmapFromUri(uri)
            if (original == null) {
                Toast.makeText(this, "Image could not be loaded", Toast.LENGTH_SHORT).show()
            } else {
                val corrector = ColorCorrector(original)
                val filter = corrector.underwaterFilter()
                corrector.applyFilter(filter)
                bitmap = corrector.bitmap
            }
        }.start()
    }

    @Composable
    fun ImageContent(image: ImageBitmap) {
        Image(
            bitmap = image,
            contentDescription = "Color corrected image"
        )
    }

    @Composable
    fun LoadingContent() {
        Box(contentAlignment = Alignment.Center) {
            LinearProgressIndicator()
        }
    }

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(uri, "r") ?: return null
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val opts = BitmapFactory.Options().apply {
            inMutable = true
        }
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, opts)
        parcelFileDescriptor.close()
        return image
    }

    private fun saveBitmap(bitmap: Bitmap, originalUri: Uri) {
        val oldFileName = originalUri.lastPathSegment ?: "unknown"
        val filePath = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/DiveColorCorrector"
        val dir = File(filePath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "${sanitizeFileName(oldFileName)}_corrected.png")
        val fOut = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fOut)
        fOut.flush()
        fOut.close()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }
}