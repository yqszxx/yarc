// See README.md for license details.

package yarc.stages

import chisel3._


class ExecuteStageControlSignals extends Bundle {
  import yarc.elements.ALU.ALUOperation
  val aluOperation    = ALUOperation()
  import yarc.elements.BranchConditionGenerator.BranchCondition
  val branchCondition = BranchCondition()
}

class ExecuteIO extends Bundle {

  val control = Input(new ExecuteStageControlSignals)

  val data = new Bundle {
    val operator1       = Input(UInt(32.W))
    val operator2       = Input(UInt(32.W))
    val registerSource1 = Input(UInt(32.W))
    val registerSource2 = Input(UInt(32.W))
    val aluResult       = Output(UInt(32.W))
    val branchTaken     = Output(Bool())
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

  import yarc.elements.BranchConditionGenerator
  val branchConditionGenerator = Module(new BranchConditionGenerator)
  branchConditionGenerator.io.condition := io.control.branchCondition
  branchConditionGenerator.io.operator1 := io.data.registerSource1
  branchConditionGenerator.io.operator2 := io.data.registerSource2
  io.data.branchTaken := branchConditionGenerator.io.result
}
