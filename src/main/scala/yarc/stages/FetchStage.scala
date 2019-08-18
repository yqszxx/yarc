// See README.md for license details.

package yarc.stages

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object FetchStage {
  object ProgramCounterSource extends ChiselEnum  {
    val pcPlus4,
        branchTarget,
        jalrTarget,
        exceptionVector = Value
  }
}

object FetchStageIO extends Bundle {
  import FetchStage._

  val control = new Bundle {
//    val pcSource = Input(ProgramCounterSource())
//    val stall    = Input(Bool())
//    val kill     = Input(Bool())
  }

  val data = new Bundle {
//    val branchTarget             = Input(UInt(32.W))
//    val jalrTarget               = Input(UInt(32.W))
//    val exceptionVector          = Input(UInt(32.W))
    val pc                       = Output(UInt(32.W))
    val instruction              = Output(UInt(32.W))
  }

  import yarc.elements.MemoryReadOnlyPort
  val mem = Flipped(new MemoryReadOnlyPort)
}

class FetchStage extends Module {
  import FetchStage._
  val io = IO(FetchStageIO)

  val programCounter = RegInit("h0000".U(32.W))
  val pcPlus4 = programCounter + 4.U

  // program counter source mux
//  val pcSource = MuxLookup(
//    io.control.pcSource.asUInt,
//    pcPlus4,
//    Array(
//      ProgramCounterSource.pcPlus4.asUInt -> pcPlus4,
//      ProgramCounterSource.branchTarget.asUInt -> io.data.branchTarget,
//      ProgramCounterSource.jalrTarget.asUInt -> io.data.jalrTarget,
//      ProgramCounterSource.exceptionVector.asUInt -> io.data.exceptionVector
//    )
//  )
  val pcSource = pcPlus4

  //  when (!io.control.stall) {
  programCounter := pcSource
  //  }

  io.data.pc := programCounter
  io.mem.address := programCounter

//  val bubble = "h00000013".U(32.W) // add x0, x0, x0
  // instruction mux
//  val instruction = Mux(io.control.kill, bubble, io.mem.readData)
//  io.data.instruction := instruction
  io.data.instruction := io.mem.readData
  printf(p"Fetching Instruction: 0x${Hexadecimal(io.mem.address)} -> 0x${Hexadecimal(io.data.instruction)}\n")
}
