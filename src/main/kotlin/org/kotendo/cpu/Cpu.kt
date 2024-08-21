package org.kotendo.cpu

import org.kotendo.Address
import org.kotendo.Interrupt
import org.kotendo.toUint
import java.lang.RuntimeException

class Cpu(
    private val cpuBus: Bus,
    private val interrupt: Interrupt
) {
    internal val register = Register()
    internal val stack = Stack(bus = cpuBus, register = register)

    fun reset() {
        register.pc = cpuBus.readWord(Address(0xFFFC))
        register.sp = 0xFF
        register.statusInterruptDisabled = true
        register.statusDecimalMode = false
    }

    private fun processNmi() {
        register.statusBreakMode = false
        val pc = register.pc
        stack.push((pc shr 8).toByte())
        stack.push((pc and 0xFF).toByte())
        stack.push(register.sp.toByte())
        register.statusInterruptDisabled = true
        val addr = cpuBus.readWord(Address(0xFFFA))
        register.pc = addr
    }

    fun fetch(): Int {
        val byte = cpuBus.read(register.pcAsAddress())
        register.pc++
        return byte
    }

    fun fetchWord(): Int {
        val word = cpuBus.readWord(register.pcAsAddress())
        register.pc++
        register.pc++
        return word
    }

    fun fetchAsAddress() = Address(fetch())

    fun fetchWordAsAddress() = Address(fetchWord())

    private fun fetchOperand(addressingMode: AddressingMode): Operand {
        return when (addressingMode) {
            AddressingMode.IMPLICIT -> Operand()
            AddressingMode.ACCUMULATOR -> Operand(
                valueFunction = { register.a }
            )
            AddressingMode.IMMEDIATE -> fetch().let { value -> Operand(raw = value, valueFunction = {value}) }

        }

    }

}