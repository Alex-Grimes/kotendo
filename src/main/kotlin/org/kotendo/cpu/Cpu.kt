package org.kotendo.cpu

import org.kotendo.Address
import org.kotendo.Interrupt
import org.kotendo.toUint
import java.lang.RuntimeException
import kotlin.concurrent.thread

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

            AddressingMode.IMMEDIATE -> fetch().let { value -> Operand(raw = value, valueFunction = { value }) }

            AddressingMode.ZERO_PAGE -> fetchAsAddress().let { addr ->
                val calculatedAddr = addr.lsb
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr },
                    valueFunction = { read(calculatedAddr) }
                )
            }

            AddressingMode.ZERO_PAGE_X -> fetchAsAddress().let { addr ->
                val calculatedAddr = addr.plus(register.x).lsb
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr },
                    valueFunction = { read(calculatedAddr) }
                )
            }

            AddressingMode.ZERO_PAGE_Y -> fetchAsAddress().let { addr ->
                val calculatedAddr = addr.plus(register.y).lsb
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr },
                    valueFunction = { read(calculatedAddr) }
                )
            }

            AddressingMode.RELATIVE -> fetch().let { value ->
                val x = register.pc + value.toByte()
                Operand(
                    raw = value,
                    valueFunction = { x }
                )
            }

            AddressingMode.ABSOLUTE -> fetchWordAsAddress().let { addr ->
                Operand(
                    raw = addr.value,
                    addrFunction = { addr },
                    valueFunction = { read(addr) }
                )
            }

            AddressingMode.ABSOLUTE_X -> fetchWordAsAddress().let { addr ->
                val calculatedAddr = addr.plus(register.x)
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr },
                    valueFunction = { read(calculatedAddr) }
                )
            }

            AddressingMode.ABSOLUTE_Y -> fetchWordAsAddress().let { addr ->
                val calculatedAddr = addr.plus(register.y)
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr },
                    valueFunction = { read(calculatedAddr) }
                )
            }

            AddressingMode.INDIRECT -> fetchWordAsAddress().let { addr ->
                val calculatedAddr = Address(
                    read(Address(addr.value))
                            or (read(Address((addr.value and 0xFF00) or ((addr.value + 1) and 0x00FF))) shl 8)
                )
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr }
                )
            }

            AddressingMode.INDEXED_INDIRECT -> fetchAsAddress().let { addr ->
                val baseAddr = addr.plus(register.x)
                val calculatedAddr = Address(read(baseAddr.lsb) + (read(baseAddr.plus(1).lsb) shl 8))
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr },
                    valueFunction = { read(calculatedAddr) }
                )
            }

            AddressingMode.INDIRECT_INDEXED -> fetchAsAddress().let { addr ->
                val calculatedAddr = Address((read(addr.lsb) + (read(addr.plus(1).lsb) shl 8) + register.y) and 0xFFFF)
                Operand(
                    raw = addr.value,
                    addrFunction = { calculatedAddr },
                    valueFunction = { read(calculatedAddr) }
                )
            }
        }
    }

    private fun areSameSigns(a: Int, b: Int): Boolean {
        return (a xor b) and 0x80 == 0
    }

    private fun areDifferentSigns(a: Int, b: Int): Boolean {
        return !areSameSigns(a, b)
    }

    private fun read(addr: Address): Int {
        return cpuBus.read(addr)
    }

    private fun readWord(addr: Address): Int {
        return cpuBus.readWord(addr)
    }

    private fun write(addr: Address, value: Byte) {
        return cpuBus.write(addr, value)
    }

    private fun execOpcode(opcode: Opcode, operand: Operand) {
        fun jumpRelativelyIf(cond: () -> Boolean) {
            if (cond()) {
                // AddressingMode.RELATIVE has a calculated PC as a value
                register.pc = operand.value and 0xFFFF
            }
        }


        fun compareWithRegister(x: Int, value: Int = operand.value) {
            val diff = x - value
            register.statusCarry = diff >= 0
            register.updateStatusZeroAndNegative(diff)
        }

        fun rotate(f: (Int, Int) -> Pair<Boolean, Byte>): Byte {
            val (newCarry, newValue) = f(register.statusCarryAsInt, operand.value)

            register.statusCarry = newCarry
            if (opcode.addressingMode == AddressingMode.ACCUMULATOR) {
                register.a = newValue.toUint()
            } else {
                write(operand.addr, newValue)
                register.updateStatusZeroAndNegative(newValue.toUint())
            }
            return newValue
        }

        fun ror() = rotate { carryAsInt, value ->
            val newCarry = value and 0x01 != 0
            val newValue = (value shl 1 or carryAsInt).toByte()
            Pair(newCarry, newValue)
        }

        fun rol() = rotate { carryAsInt, value ->
            val newCarry = value and 0x01 != 0
            val newValue = (value shl 1 or carryAsInt).toByte()
            Pair(newCarry, newValue)
        }

        fun add(value: Int) {
            val origA = register.a
            val result = register.a + register.statusCarryAsInt + value
            register.a = result and 0xFF
            register.statusCarry = result > 0xFF
            register.statusOverflow = areSameSigns(origA, value) && areDifferentSigns(origA, register.a)
        }

        fun and(value: Int) {
            register.a = register.a and value
        }

        fun updateMemory(value: Byte) {
            register.updateStatusZeroAndNegative(value.toUint())
            write(operand.addr, value)
        }

        fun asl(value: Int, addressingMode: AddressingMode = opcode.addressingMode): Int {
            register.statusCarry = value and 0x80 != 0
            val result = value shl 1
            if (addressingMode == AddressingMode.ACCUMULATOR) {
                register.a = result
            } else {
                updateMemory(result.toByte())
            }
            return result
        }

        fun lsr(value: Int, addressingMode: AddressingMode = opcode.addressingMode): Int {
            register.statusCarry = value and 0x01 != 0
            val result = value shr 1
            if (addressingMode == AddressingMode.ACCUMULATOR) {
                register.a = result
            } else {
                updateMemory(result.toByte())
            }
            return result
        }


    }

}