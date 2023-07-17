package com.davidlang.divecolorcorrector

import android.graphics.Color

data class IntColor(val r: Int, val g: Int, val b: Int) {
    constructor(rgb: Int) : this(Color.red(rgb), Color.green(rgb), Color.blue(rgb))

    fun sum(): Int {
        return r + g + b
    }
}