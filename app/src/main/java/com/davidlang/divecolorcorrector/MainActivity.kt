package com.davidlang.divecolorcorrector

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
        val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isNotEmpty()) {
                Toast.makeText(this, "STUB: ${uris.size} files", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "STUB: No media", Toast.LENGTH_LONG).show()
            }
        }
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }
}