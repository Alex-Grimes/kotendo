package org.kotendo.ppu

import javafx.scene.image.WritableImage
import org.kotendo.Address
import org.kotendo.toUint
import org.kotendo.Interrupt
import org.kotendo.rom.ChrRom

class Ppu(
    private val interrupt: Interrupt,
    private val nametableMirroringVertical: Boolean,
    ramBytes: ByteArray
) {
    companion object {
        const val sizeOfSpriteData = 16

        const val cyclesPerLine = 341

        const val zoomRate = 4
        const val bitsInChar = 8
        const val bytesOfOneColorSprite = 8

        const val registerAddrOfWriteStatus0 = 0x0000
        const val registerAddrOfWriteStatus1 = 0x0001
        const val registerAddrOfReadStatus = 0x0002
        const val registerAddrOfSpriteRamAddr = 0x0003
        const val registerAddrOfSpriteRamData = 0x0004
        const val registerAddrOfScrollOffset = 0x0005
        const val registerAddrOfPpuAddr = 0x0006
        const val registerAddrOfPpuData = 0x0007

        val addrOfPatternTable0 = Address(0x0000)
        val addrOfPatternTable1 = Address(0x1000)
        val addrOfNameTable = Address(0x2000)
        val addrOfAttrTable = Address(0x23C0)
        val addrOfBgPaletteTable = Address(0x3F00)
        val addrOfSpritePaletteTable = Address(0x3F10)

        fun from(
            interrupt: Interrupt,
            nametableMirroringVertical: Boolean,
            chrRom: ChrRom
            ): Ppu {
            val bytes = ByteArray(0x4000)
            chrRom.copyInto(bytes)
            return Ppu(
                interrupt = interrupt,
                nametableMirroringVertical = nametableMirroringVertical,
                ramBytes = bytes
            )
        }
    }

    val image = createImage()

    private val spriteRam = SpriteRam()
    private val ram = PpuRam(ramBytes)
    private val register = Register()
    private val renderer = Renderer(ram, Colors(), image)

    private var cycle = 0
    private var line = Line(0)
    private var tileY = Tile.Unit(0)

    private val scroll = Scroll(register)

    private fun createImage(): WritableImage {
        return WritableImage(Pixel.maxX * zoomRate, Pixel.maxY * zoomRate)
    }

   fun read(addr: Address): Int {
       val result = when (addr.value) {
           registerAddrOfReadStatus -> {
               register.readByte.toUint().let { orig ->
                   register.vblank = false
                   orig
               }
           }
           registerAddrOfSpriteRamData -> spriteRam.read()
           registerAddrOfPpuData -> ram.read(register.ppuAddrIncrBytesb)
           else -> throw IllegalArgumentException("Read operation is invalid at addr:$addr")
       }
       return result
   }

    fun write(addr: Address, value: Byte) {
        when (addr.value) {
            registerAddrOfWriteStatus0 -> register.writeByte0 = value.toUint()
            registerAddrOfWriteStatus1 -> register.writeByte1 = value.toUint()
            registerAddrOfSpriteRamAddr -> spriteRam.addr(Address(value.toUint()))
            registerAddrOfSpriteRamData -> spriteRam.write(value)
            registerAddrOfPpuAddr -> ram.addr(Address(value.toUint()))
            registerAddrOfScrollOffset -> scroll.write(value)
            registerAddrOfPpuData -> ram.write(value, register.ppuAddrIncrBytes)
            else -> throw IllegalArgumentException("Write operation is not permitted at addr:$addr")
        }
    }

    fun transferToSprite(bytes: ByteArray) {
        spriteRam.transfer(bytes)
    }

    private fun buildBgLine() {
        val baseY = tileY.toPixel()
        val absoluteY = scroll.absoluteY(baseY)
        val displayY = scroll.displayY(baseY)

        for (tileXIndex in 0.until(Tile.maxX + 1)){
            val baseX = Tile.Unit(tileXIndex).toPixel()
            val absoluteX = scroll.absoluteX(baseX)
            val displayX = scroll.displayX(baseX)

            val nameTableId = nametableId(Pixel(absoluteX, absoluteY))
            val normalizedTile = Tile(
                x = Tile.Unit.fromPixel(Pixel.Unit(absoluteX.value % Pixel.maxX)),
                y = Tile.Unit.fromPixel(Pixel.Unit(absoluteX.value % Pixel.maxY))
            )
            val spriteId = ram.spriteId(addrOfNameTable(nameTableId),normalizedTile)

            val paletteId = ram.attribute(
                addrOfAttrTable = addrOfAttrTable(nametableId = nameTableId),
                tile = normalizedTile
            ).paletteId(tile = normalizedTile)

            renderer.add(
                layer = Renderer.Layer.BACKGROUND,
                addrOfPatternTable = if (register.bgPatternAddrMode){
                    addrOfPatternTable1
                }
                else {
                    addrOfPatternTable0
                },
                addrOfPaletteTable = addrOfBgPaletteTable,
                spriteId = spriteId,
                paletteId = paletteId,
                position = Pixel(displayX, displayY)
            )
        }
    }

}