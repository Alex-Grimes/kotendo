package org.kotendo.cpu

internal data class NotSupportedOperation(

    val msg: String,
   //#TODO need to implement OpCode
    // val opcode: Opcode? = null,
    val operand: Operand? = null
): Exception() {
    override val message: String
        get() {
            return this.toString()
        }
}

