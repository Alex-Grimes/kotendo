package org.kotendo.rom

import org.kotendo.Address
import org.kotendo.toUint

class PrgRom(private val bytes: ByteArray) {
    val size: Int
        get() = bytes.size

    fun read(addr: Address): Int {
        return bytes[addr.value].toUint()
    }
}

class ChrRom(private val bytes: ByteArray) {
    fun copyInto(dest: ByteArray) {
        bytes.copyInto(dest)
    }
}