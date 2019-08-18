// See README.md for license details.

package yarc.stages

import chisel3._
import chisel3.experimental.ChiselEnum


object MemoryStage {
  object MemoryIsWriting extends ChiselEnum {
    val no  = Value(false.B)
    val yes = Value(true.B)
  }
}

class MemoryStageControlSignals extends Bundle {
  import MemoryStage._
  val isWriting = MemoryIsWriting()
}

class MemoryStageIO extends Bundle {
  val control = Input(new MemoryStageControlSignals)

  val data = new Bundle {
    val writeData = Input(UInt(32.W))
    val address   = Input(UInt(32.W))
    val readData  = Output(UInt(32.W))
  }

  import yarc.elements.MemoryReadWritePort
  val mem = Flipped(new MemoryReadWritePort)
}

class MemoryStage extends Module {
  val io = IO(new MemoryStageIO)

  io.mem.address := io.data.address
  io.mem.writeData := io.data.writeData
  io.mem.writeEnable := io.control.isWriting.asUInt.asBool
  io.data.readData := io.mem.readData
}
