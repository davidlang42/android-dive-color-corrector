package com.davidlang.divecolorcorrector

data class Interval(val low: Int, val high: Int) {
    fun diff(): Int {
        return high - low
    }
}