package org.kotendo.cpu

import org.kotendo.Address

internal class Operand(
    private val raw: Int? = null,
    private val addrFunction: (() -> Address)? = null,
    private val valueFunction: (() -> Int)? = null
) {
    val addr : Address by lazy {
        val f = addrFunction

        if (f == null) {
            throw NotSupportedOperation (
                msg = "this operand doesn't have an address"
            )
        }
        else {
            f()
        }

    }

    val value : Int by lazy {
        val f = valueFunction

        if (f == null) {
            throw NotSupportedOperation(
                msg = "This operand doesn't have a value"
            )
        }
        else {
            f()
        }
    }

    override fun toString(): String {
        val addr = addrFunction.let {
            if (it == null) "null" else  "0x%04X".format(it)
        }
        val raw = raw.let {
            if (it == null) "null" else "0x%04X".format(it)
        }
        return "Operand(addr=$addr, raw=$raw)"
    }
}