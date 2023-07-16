package com.davidlang.divecolorcorrector

import android.graphics.Bitmap
import android.graphics.Color

class ColorCorrector(var bitmap: Bitmap) {
    fun correctColors() {
        Thread.sleep(1000) // STUB: actually correct the image
        invertColors()
    }

    fun invertColors() {
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val old = bitmap.getPixel(x, y)
                val new = Color.argb(Color.alpha(old), 255-Color.red(old), 255-Color.green(old), 255-Color.blue(old))
                bitmap.setPixel(x, y, new)
            }
        }
    }
}