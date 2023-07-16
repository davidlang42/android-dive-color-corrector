package com.davidlang.divecolorcorrector

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlang.divecolorcorrector.ui.theme.DiveColorCorrectorTheme
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
        val bitmap = getBitmapFromUri(uri)
        if (bitmap == null) {
            Toast.makeText(this, "Image uri could not be loaded", Toast.LENGTH_LONG).show()
            return
        }
        setContent {
            DiveColorCorrectorTheme {
                Box(
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Color corrected image"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text("Cancel", fontSize = 20.sp)
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text("Save a copy", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(uri, "r") ?: return null
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }
}