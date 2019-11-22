// See README.md for license details.

package yarc.stages

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object FetchStage {
  object ProgramCounterSource extends ChiselEnum  {
    val pcPlus4,
        branchTarget = Value
  }
}

class FetchStageControlSignals extends Bundle {
  import yarc.stages.FetchStage.ProgramCounterSource
  val pcSource    = ProgramCounterSource()

  def defaults(): Unit = {
    pcSource := ProgramCounterSource.pcPlus4
  }
}

class FetchStageIO extends Bundle {
  val control = Input(new FetchStageControlSignals)

  val data = new Bundle {
    val branchTarget  = Input(UInt(32.W))
    val branchTaken   = Input(Bool())
    val pc            = Output(UInt(32.W))
    val instruction   = Output(UInt(32.W))
  }

  val kill = Input(Bool())
  val stall = Input(Bool())

  import yarc.elements.MemoryReadOnlyPort
  val mem = Flipped(new MemoryReadOnlyPort)
}

class FetchStage extends Module {
  import yarc.stages.FetchStage.ProgramCounterSource
  val io = IO(new FetchStageIO)

  val programCounter = RegInit("h0000".U(32.W))
  val pcPlus4 = programCounter + 4.U

  // program counter source mux
  val pcSource = MuxLookup(
    io.control.pcSource.asUInt,
    pcPlus4,
    Array(
      ProgramCounterSource.pcPlus4.asUInt      -> pcPlus4,
      ProgramCounterSource.branchTarget.asUInt -> Mux(io.data.branchTaken, io.data.branchTarget, pcPlus4)
    )
  )

  when (!io.stall && !io.kill) {
    programCounter := pcSource
  } .otherwise {
    programCounter := programCounter
  }

  io.mem.address := programCounter

  val killPC = "hFFFE".U(32.W)
  // kill logic
  val kill = io.kill || (io.control.pcSource === ProgramCounterSource.branchTarget && io.data.branchTaken)
  when (!kill) {
    io.data.pc := programCounter
    io.data.instruction := io.mem.readData
  } .otherwise {
    io.data.pc := killPC
    io.data.instruction := 0x4033.U(32.W) // XOR x0,x0,x0
  }

  printf(p"Fetching Instruction: 0x${Hexadecimal(io.mem.address)} -> 0x${Hexadecimal(io.data.instruction)}\n")
}
