package com.davidlang.divecolorcorrector

import android.R.attr
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlang.divecolorcorrector.ui.theme.DiveColorCorrectorTheme
import java.io.FileDescriptor
import java.io.IOException


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiveColorCorrectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .clickable(onClick = { showFilePicker() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Click to select an underwater photo or video to color correct",
                            textAlign = TextAlign.Center,
                            fontSize = 36.sp,
                            lineHeight = 36.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    private fun showFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/jpeg"
        }

        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                PICK_IMAGE -> {
                    val src: String? = data?.data?.toString()
                    if (src == null) {
                        Toast.makeText(this, "Missing file path", Toast.LENGTH_SHORT).show()
                    } else {
                        val bitmap = getBitmapFromUri(src)
                        if (bitmap == null) {
                            Toast.makeText(this, "Image $src could not be loaded", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "STUB: width: ${bitmap.width} height: ${bitmap.height}", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                else -> throw Exception("Invalid request code: $requestCode")
            }
        }
    }

    @Throws(IOException::class)
    private fun getBitmapFromUri(src: String): Bitmap? {
        val uri = Uri.parse(src)
        val parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(uri, "r") ?: return null
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }
}

const val PICK_IMAGE = 1;