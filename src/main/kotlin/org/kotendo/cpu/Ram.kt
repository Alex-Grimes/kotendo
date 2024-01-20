package org.kotendo.cpu

import org.kotendo.Address
import org.kotendo.toUint

class Ram {
    private val bytes = ByteArray(0x0800)

    fun read(addr: Address): Int {
        return bytes[addr.value].toUint()
    }

    fun write(addr: Address, value: Byte) {
        bytes[addr.value] = value
    }

    fun slice(addr: Address, size: Int): ByteArray {
        return bytes.sliceArray(addr.value.until(addr.value + size))
    }
}