// See README.md for license details.

package yarc.stages

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.MuxLookup


object WritebackStage {
  object RegisterIsWriting extends ChiselEnum {
    val no  = Value(false.B)
    val yes = Value(true.B)
  }
  object WritebackSource extends ChiselEnum  {
    val aluResult,
        memoryReadData,
        pcPlus4 = Value
    val notRelated = aluResult
  }
}

class WritebackStageControlSignals extends Bundle {
  import WritebackStage._
  val isWriting = RegisterIsWriting()
  val writebackSource = WritebackSource()
}

class WritebackStageIO extends Bundle {
  val control = Input(new WritebackStageControlSignals)

  val data = new Bundle {
    val writeRegister  = Input(UInt(5.W))
    val aluResult      = Input(UInt(32.W))
    val memoryReadData = Input(UInt(32.W))
    val pc             = Input(UInt(32.W))
  }

  import yarc.elements.RegisterFileWritePort
  val registers = Flipped(new RegisterFileWritePort)
}

class WritebackStage extends Module {
  import WritebackStage.WritebackSource
  val io = IO(new WritebackStageIO)

  io.registers.isWriting := io.control.isWriting.asUInt.asBool
  io.registers.writeRegister := io.data.writeRegister
  io.registers.writeData := MuxLookup(
    io.control.writebackSource.asUInt,
    io.data.aluResult,
    Array(
      WritebackSource.aluResult.asUInt      -> io.data.aluResult,
      WritebackSource.memoryReadData.asUInt -> io.data.memoryReadData,
      WritebackSource.pcPlus4.asUInt        -> (io.data.pc + 4.U)
    )
  )
}
