// See README.md for license details.

package yarc.elements

import chisel3._
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

  printf(p"Mem1: ${Hexadecimal(io.port1.address)}\n")
  printf(p"Mem2: ${Hexadecimal(io.port2.address)}\n")

  // assert the addresses are word aligned
  assert(io.port1.address(0) === 0.U && io.port1.address(1) === 0.U)
  assert(!io.port2.writeEnable || (io.port2.address(0) === 0.U && io.port2.address(1) === 0.U))

  val memory = Mem(0xffff, UInt(32.W))
  loadMemoryFromFile(memory, "mem.txt")

  val a = io.port1.address(31, 2)
  val actualAddress1 = io.port1.address(31, 2)
  io.port1.readData := memory.read(actualAddress1)

  val actualAddress2 = io.port2.address(31, 2)
  io.port2.readData := memory.read(actualAddress2)
  when (io.port2.writeEnable) {
    memory.write(actualAddress2, io.port2.writeData)
  }
}
