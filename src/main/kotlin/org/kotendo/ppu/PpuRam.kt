package org.kotendo.ppu

import org.kotendo.Address
import org.kotendo.toUint

internal class PpuRam(private val bytes: ByteArray) {
    private var addr: Address = Address(0)
    private var settingHigherByteAddr = true



    private fun adjustedAddrForRead(addr: Address): Address {
        return when (addr.value) {
            0x3F04 -> Address(0x3F00)
            0x3F08 -> Address(0x3F00)
            0x3F0C -> Address(0x3F00)
            0x3F10 -> Address(0x3F00)
            0x3F14 -> Address(0x3F04)
            0x3F18 -> Address(0x3F08)
            0x3F1C -> Address(0x3F0C)
            else -> addr
        }
    }

    private fun adjustedAddrForWrite(addr: Address): Address {
        return when (addr.value) {
            0x3F10 -> Address(0x3F00)
            0x3F14 -> Address(0x3F04)
            0x3F18 -> Address(0x3F08)
            0x3F1C -> Address(0x3F0C)
            else -> addr
        }
    }

    fun addr(addr: Address) {
        if (settingHigherByteAddr) {
            this.addr = Address(addr.lsb.value shl 8 )
            settingHigherByteAddr = false
        }
        else {
            this.addr = this.addr.plus(addr)
            settingHigherByteAddr = true
        }
    }

    private var bufferedValue = 0
    fun read(addrIncr: Int): Int {
        val adjustedAddr = adjustedAddrForRead(addr)
        if (adjustedAddr.value >= 0x3F00) {
            return bytes[adjustedAddr.value].toUint()
        }
        val value = bufferedValue
        bufferedValue = bytes[adjustedAddr.value].toUint()
        addr = addr.plus(addrIncr)
        return value
    }

    fun write(value: Byte, addrIncr: Int){
        val adjustedAddr = adjustedAddrForWrite(addr)
        bytes[adjustedAddr.value] = value
        addr = addr.plus(addrIncr)
    }

     fun spriteData(addrOfPatternTable: Address, spriteId: Int): ByteArray {
         val startAddr = addrOfPatternTable.plus(Ppu.sizeOfSpriteData * spriteId)
         val endAddr = addrOfPatternTable.plus(Ppu.sizeOfSpriteData * (spriteId + 1))
         return bytes.sliceArray(startAddr.value.until(endAddr.value))
     }

    fun paletteData(addrOfPaletteTable: Address, paletteId: Int): ByteArray {
        val startAddr = addrOfPaletteTable.plus(paletteId * 4)
        val endAddr = addrOfPaletteTable.plus((paletteId + 1) * 4)
        return startAddr.value.until(endAddr.value).map {
            bytes[adjustedAddrForRead(Address(it)).value]
        }.toByteArray()
    }

    fun spriteId(addrOfNameTable: Address, tile: Tile): Int {
        return bytes[addrOfNameTable.plus(tile.y.value * Tile.maxX).plus(tile.x.value).value].toUint()
    }

    fun attribute(addrOfAttrTable: Address, tile: Tile): Attribute {
        val attrX = tile.x.toAttribute()
        val attrY = tile.y.toAttribute()
        return Attribute(
            x = attrX,
            y = attrY,
            value = bytes[addrOfAttrTable.plus(attrY.value * Attribute.maxX).plus(attrX.value).value].toUint()
        )
    }
}