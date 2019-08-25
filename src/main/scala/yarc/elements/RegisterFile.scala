// See README.md for license details.

package yarc.elements

import chisel3._

class RegisterFileReadPort extends Bundle {
  val readRegister1 = Input(UInt(5.W)) // TODO: find a better way to express register number
  val readRegister2 = Input(UInt(5.W))
  val readData1     = Output(UInt(32.W))
  val readData2     = Output(UInt(32.W))
}

class RegisterFileWritePort extends Bundle {
  val writeRegister = Input(UInt(5.W))
  val writeData     = Input(UInt(32.W))
  val isWriting     = Input(Bool())
}

class RegisterFileIO extends Bundle {
  val readPort = new RegisterFileReadPort
  val writePort = new RegisterFileWritePort
}
/**
  * Registers
  */
class RegisterFile extends Module {
  val io = IO(new RegisterFileIO)

  val numberOfRegisters = 32
  val registers = Reg(Vec(numberOfRegisters, UInt(32.W)))

  io.readPort.readData1 := Mux(io.readPort.readRegister1 =/= 0.U, registers(io.readPort.readRegister1), 0.U)
  io.readPort.readData2 := Mux(io.readPort.readRegister2 =/= 0.U, registers(io.readPort.readRegister2), 0.U)

  when (io.writePort.isWriting) {
    when (io.writePort.writeRegister =/= 0.U) {
      registers(io.writePort.writeRegister) := io.writePort.writeData
    }
  }
}
