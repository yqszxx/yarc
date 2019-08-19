// See README.md for license details.

package yarc.stages

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object FetchStage {
  object ProgramCounterSource extends ChiselEnum  {
    val pcPlus4 = Value
  }
}

class FetchStageControlSignals extends Bundle {
}

class FetchStageIO extends Bundle {
  val control = new FetchStageControlSignals

  val data = new Bundle {
    val pc                       = Output(UInt(32.W))
    val instruction              = Output(UInt(32.W))
  }

  import yarc.elements.MemoryReadOnlyPort
  val mem = Flipped(new MemoryReadOnlyPort)
}

class FetchStage extends Module {
  val io = IO(new FetchStageIO)

  val programCounter = RegInit("h0000".U(32.W))
  val pcPlus4 = programCounter + 4.U

  val pcSource = pcPlus4

  programCounter := pcSource

  io.data.pc := programCounter
  io.mem.address := programCounter

  io.data.instruction := io.mem.readData
  printf(p"Fetching Instruction: 0x${Hexadecimal(io.mem.address)} -> 0x${Hexadecimal(io.data.instruction)}\n")
}
