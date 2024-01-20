package org.kotendo
    fun Byte.toUint(): Int {
        return this.toInt() and 0xFF
    }
