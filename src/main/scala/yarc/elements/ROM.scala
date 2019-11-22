package yarc.elements

import chisel3._

class ROM extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val data = Output(UInt(32.W))
  })

  val rom = VecInit((
    "FE000197\n00018193\n00000293\n000233B7\n21E38393\n00000313\n00729863\nFFF34313\n0061A023\n00000293\n00128293\nFEDFF06F"
  ).split("\n").map(d => ("h" + d).U(32.W)))

  io.data := rom(io.address(31, 2))
}
