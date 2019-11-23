package yarc.elements

import chisel3._

class ROM extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val data = Output(UInt(32.W))
  })

  val rom = VecInit((
    "01000117\n1FF10113\n038000EF\n0000006F\nFE010113\n00812E23\n02010413\nFEA42623\nFE000797\nFE078793\nFEC42703\n00E7A023\n00000013\n01C12403\n02010113\n00008067\nFE010113\n00112E23\n00812C23\n02010413\nFE042623\nFE042423\nFEC42703\n000707B7\n9B778793\n02F71063\nFE842503\nFA5FF0EF\nFE842783\nFFF7C793\nFEF42423\nFE042623\nFD9FF06F\nFEC42783\n00178793\nFEF42623\nFC9FF06F"
  ).split("\n").map(d => ("h" + d).U(32.W)))

  io.data := rom(io.address(31, 2))
}
