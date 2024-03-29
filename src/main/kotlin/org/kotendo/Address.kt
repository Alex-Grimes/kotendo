package org.kotendo

data class Address(val value: Int) {
    override fun toString(): String {
        return "Address(value=0x%04x)".format(value)
    }

    val msb: Address
        get() = Address(value shr 8)

    val lsb: Address
        get() = Address(value and 0xFF)

    fun plus(other: Int) = Address((this.value + other) and 0xFFFF)

    fun plus(other: Address) = Address((this.value + other.value) and 0xFFFF)

    fun minus(other: Int) = Address((this.value - other) and 0xFFFF)

    fun minus(other: Address) = Address((this.value - other.value) and 0xFFFF)

}