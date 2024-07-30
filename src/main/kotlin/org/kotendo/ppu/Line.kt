package org.kotendo.ppu

internal data class Line(val value: Int) {
    companion object {
        const val visibleLines = 240
        const val vblankLines = 22
    }

    fun incr(value: Int) = Line(this.value + value)

    fun LineTiming() = value <= visibleLines && value % 8 == 0

    fun vblankTIming() = value == visibleLines + 1

    fun readyForNewLIne() = value >= visibleLines + vblankLines
}