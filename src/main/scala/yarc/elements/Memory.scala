// See README.md for license details.

package yarc.elements

import chisel3._
import chisel3.util
import chisel3.util.experimental.loadMemoryFromFile

class MemoryReadOnlyPort extends Bundle {
  val address  = Input(UInt(32.W))
  val readData = Output(UInt(32.W))
}

class MemoryReadWritePort extends Bundle{
  val address     = Input(UInt(32.W))
  val readData    = Output(UInt(32.W))
  val writeEnable = Input(Bool())
  val writeData   = Input(UInt(32.W))
  val writeMask   = Input(Vec(4, Bool()))
}
class MemoryIO extends Bundle {
  val port1 = new MemoryReadOnlyPort
  val port2 = new MemoryReadWritePort
}

/**
  * Registers
  */
class Memory extends Module {
  val io = IO(new MemoryIO)

  val memory = Mem(0xffff, Vec(4, UInt(8.W)))
  loadMemoryFromFile(memory, "./yarc-testsw/isa/build/yarc.txt")

  val actualAddress1 = io.port1.address(31, 2)
  val readData1 = memory.read(actualAddress1)
  io.port1.readData := util.Cat(readData1(3), readData1(2), readData1(1), readData1(0))

  val actualAddress2 = io.port2.address(31, 2)
  val writeData = io.port2.writeData
  val readData2 = memory.read(actualAddress2)
  io.port2.readData := util.Cat(readData2(3), readData2(2), readData2(1), readData2(0))
  when (io.port2.writeEnable) {
    memory.write(actualAddress2, VecInit(Seq(
      writeData(7, 0),
      writeData(15, 8),
      writeData(23, 16),
      writeData(31, 24)
    )), io.port2.writeMask)
  }
}
