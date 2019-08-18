// See README.md for license details.

package yarc.elements

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._


object ALU {
  object ALUOperation extends ChiselEnum {
    val add,
        sub,
        sll,
        srl,
        sra,
        and,
        or,
        xor,
        slt,
        sltu,
//        copy1,
        copy2 = Value
    val notRelated = add
  }
}


/**
  * Arithmetic Logic Unit
  */
class ALU extends Module {
  import ALU._
  val io = IO(new Bundle {
    val operator1 = Input(UInt(32.W))
    val operator2 = Input(UInt(32.W))
    val operation = Input(ALUOperation())
    val result    = Output(UInt(32.W))
  })

  val shiftAmount = io.operator2(4,0).asUInt

  io.result := MuxLookup(
    io.operation.asUInt,
    0.U(32.W),
    Array(
      ALUOperation.add.asUInt   -> (io.operator1 + io.operator2)(31, 0),
      ALUOperation.sub.asUInt   -> (io.operator1 - io.operator2).asUInt,
      ALUOperation.and.asUInt   -> (io.operator1 & io.operator2).asUInt,
      ALUOperation.or.asUInt    -> (io.operator1 | io.operator2).asUInt,
      ALUOperation.xor.asUInt   -> (io.operator1 ^ io.operator2).asUInt,
      ALUOperation.slt.asUInt   -> (io.operator1.asSInt < io.operator2.asSInt).asUInt,
      ALUOperation.sltu.asUInt  -> (io.operator1 < io.operator2).asUInt,
      ALUOperation.sll.asUInt   -> (io.operator1 << shiftAmount)(31, 0).asUInt,
      ALUOperation.sra.asUInt   -> (io.operator1.asSInt >> shiftAmount).asUInt,
      ALUOperation.srl.asUInt   -> (io.operator1 >> shiftAmount).asUInt,
//      ALUOperation.copy1.asUInt -> io.operator1,
      ALUOperation.copy2.asUInt -> io.operator2
  ))
}
