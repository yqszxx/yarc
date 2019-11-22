package yarc.elements

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.MuxLookup

object BranchConditionGenerator {
  object BranchCondition extends ChiselEnum {
    val eq,
        ne,
        lt,
        ltu,
        ge,
        geu,
        alwaysTrue = Value
    val notRelated = eq
  }
}

class BranchConditionGenerator extends Module {
  import BranchConditionGenerator._
  val io = IO(new Bundle {
    val operator1 = Input(UInt(32.W))
    val operator2 = Input(UInt(32.W))
    val condition = Input(BranchCondition())
    val result    = Output(Bool())
  })

  io.result := MuxLookup(
    io.condition.asUInt,
    false.B,
    Array(
      BranchCondition.eq.asUInt  -> (io.operator1 === io.operator2),
      BranchCondition.ne.asUInt  -> (io.operator1 =/= io.operator2),
      BranchCondition.lt.asUInt  -> (io.operator1.asSInt < io.operator2.asSInt),
      BranchCondition.ltu.asUInt -> (io.operator1 < io.operator2),
      BranchCondition.ge.asUInt  -> (io.operator1.asSInt >= io.operator2.asSInt),
      BranchCondition.geu.asUInt -> (io.operator1 >= io.operator2),
      BranchCondition.alwaysTrue.asUInt -> true.B
    ))
}
