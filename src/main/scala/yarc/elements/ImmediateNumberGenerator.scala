// See README.md for license details.

package yarc.elements

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._


object ImmediateNumberGenerator {
  object ImmediateNumberType extends ChiselEnum {
    // immediate types
    val notRelated,
        iType,
        sType,
        bType,
        uType,
        jType,
        zType = Value
  }
}

/**
  * Immediate Number Generator
  */
class ImmediateNumberGenerator extends Module {
  import ImmediateNumberGenerator._
  val io = IO(new Bundle {
    val instruction   = Input(UInt(32.W))
    val immediateType = Input(ImmediateNumberType())
    val immediate     = Output(SInt(32.W))
  })


  // immediate for each type
  val iTypeSignExtended = Cat(
    Fill(20,io.instruction(31)),
    io.instruction(31, 20)
  ).asSInt

  val sTypeSignExtended = Cat(
    Fill(20,io.instruction(31)),
    io.instruction(31, 25),
    io.instruction(11, 7)
  ).asSInt

  val bTypeSignExtended = Cat(
    Fill(19,io.instruction(31)),
    io.instruction(31),
    io.instruction(7),
    io.instruction(30, 25),
    io.instruction(11, 8),
    0.U(1.W)
  ).asSInt

  val uTypeSignExtended = Cat(
    io.instruction(31, 12),
    Fill(12,0.U(1.W))
  ).asSInt

  val jTypeSignExtended = Cat(
    Fill(11,io.instruction(31)),
    io.instruction(31),
    io.instruction(19, 12),
    io.instruction(20),
    io.instruction(30, 21),
    0.U(1.W)
  ).asSInt

  val zTypeZeroExtended = Cat(
    Fill(27,0.U(1.W)),
    io.instruction(19,15)
  ).asSInt

  // determine output immediate by immediate type
  io.immediate := MuxLookup(
    io.immediateType.asUInt,
    0.S(32.W),
    Array(
      ImmediateNumberType.iType.asUInt -> iTypeSignExtended,
      ImmediateNumberType.sType.asUInt -> sTypeSignExtended,
      ImmediateNumberType.bType.asUInt -> bTypeSignExtended,
      ImmediateNumberType.uType.asUInt -> uTypeSignExtended,
      ImmediateNumberType.jType.asUInt -> jTypeSignExtended,
      ImmediateNumberType.zType.asUInt -> zTypeZeroExtended
    )
  )
}
