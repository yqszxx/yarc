package yarc

import chisel3._
import chisel3.util._
import yarc.elements._
import yarc.elements.uart.UART

class System extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val gpio = Output(Bool())
    val rxd  = Input(Bool())
    val txd  = Output(Bool())
  })

  val pipeline = Module(new Pipeline)

  val address = pipeline.io.dataMemoryPort.address
  val addressH = address(31, 24)
  val writeEnable = pipeline.io.dataMemoryPort.writeEnable
  val writeData = pipeline.io.dataMemoryPort.writeData
  val writeMask = pipeline.io.dataMemoryPort.writeMask
  val readData = pipeline.io.dataMemoryPort.readData
  readData := "hDEADBEEF".U

  val memory = Module(new Memory)
  memory.io.port1 <> DontCare
  memory.io.port2.address := address
  memory.io.port2.writeData := writeData
  memory.io.port2.writeEnable := false.B
  memory.io.port2.writeMask := writeMask

  val rom = Module(new ROM)
  rom.io.address := pipeline.io.instructionMemoryPort.address
  pipeline.io.instructionMemoryPort.readData := rom.io.data

  // Done logic
  val done = RegInit(false.B)
  io.done := done

  // GPIO logic
  val gpio = RegInit(true.B)
  io.gpio := gpio

  // UART
  val uart = Module(new UART)
  uart.io.external.rxd := io.rxd
  io.txd := uart.io.external.txd
  uart.io.address.valid := false.B
  uart.io.writePort.valid := false.B
  uart.io.readPort.ready := false.B
  uart.io.address.bits := address(3, 2)
  uart.io.writePort.bits := writeData(7, 0)

  when (writeEnable) {
    switch (addressH) {
      is ("h01".U) {
        memory.io.port2.writeEnable := true.B
      }

      is ("hFF".U) {
        done := true.B
      }

      is ("hFE".U) {
        gpio := writeData(0)
      }

      is ("hFD".U) {
        uart.io.address.valid := true.B
        uart.io.writePort.valid := true.B
      }
    }
  } otherwise { // read
    switch (addressH) {
      is ("h01".U) {
        readData := memory.io.port2.readData
      }

      is ("hFD".U) {
        uart.io.address.valid := true.B
        uart.io.readPort.ready := true.B
        readData := Cat(0.U(24.W), uart.io.readPort.bits)
      }
    }
  }
}
