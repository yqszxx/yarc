package yarc

import chisel3._
import chisel3.util._
import yarc.elements._

class System extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val gpio = Output(Bool())
  })

  val pipeline = Module(new Pipeline)

  val address = pipeline.io.dataMemoryPort.address
  val addressH = address(31, 24)
  val writeEnable = pipeline.io.dataMemoryPort.writeEnable
  val writeData = pipeline.io.dataMemoryPort.writeData
  val readData = pipeline.io.dataMemoryPort.readData
  readData := "hDEADBEEF".U

  val memory = Module(new Memory)
  memory.io.port1 <> DontCare
  memory.io.port2.address := address
  memory.io.port2.writeData := writeData
  memory.io.port2.writeEnable := false.B

  val rom = Module(new ROM)
  rom.io.address := pipeline.io.instructionMemoryPort.address
  pipeline.io.instructionMemoryPort.readData := rom.io.data

  // Done logic
  val done = RegInit(false.B)
  io.done := done

  // GPIO logic
  val gpio = RegInit(true.B)
  io.gpio := gpio

  when (writeEnable) {
    switch (addressH) {
      is ("h01".U) {
        memory.io.port2.writeEnable := true.B
      }

      is ("hFF".U) {
        done := true.B
      }

      is ("hFE".U) {
        gpio := pipeline.io.dataMemoryPort.writeData(0)
      }
    }
  } otherwise { // read
    switch (addressH) {
      is ("h01".U) {
        readData := memory.io.port2.readData
      }
    }
  }
}
