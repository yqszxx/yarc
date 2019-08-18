// See README.md for license details.

package yarc.stages

import chisel3._


class ExecuteStageControlSignals extends Bundle {
  import yarc.elements.ALU.ALUOperation
  val aluOperation = ALUOperation()
}

class ExecuteIO extends Bundle {

  val control = Input(new ExecuteStageControlSignals)

  val data = new Bundle {
    val operator1 = Input(UInt(32.W))
    val operator2 = Input(UInt(32.W))
    val aluResult = Output(UInt(32.W))
  }
}

class ExecuteStage extends Module {
  val io = IO(new ExecuteIO)

  import yarc.elements.ALU
  val alu = Module(new ALU())
  alu.io.operator1 := io.data.operator1
  alu.io.operator2 := io.data.operator2
  alu.io.operation := io.control.aluOperation
  io.data.aluResult := alu.io.result

  printf(p"Execute OP1: 0x${Hexadecimal(io.data.operator1)}; OP2: 0x${Hexadecimal(io.data.operator2)}; OP: ${Hexadecimal(io.control.aluOperation.asUInt)}\n")
  printf(p"Execute Result: 0x${Hexadecimal(io.data.aluResult)}\n")
}
