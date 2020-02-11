package yarc.elements.uart

import chisel3._
import chisel3.util._
import uart._

/**
 * Address map:
 * b00 / h00 - state
 * 7 6 5 4 3 2  1  0
 * 0 0 0 0 0 0 tx rx
 *
 * b01 / h01 - receive buffer
 * b10 / h02 - send buffer
 */
class UART extends Module {
  val io = IO(new Bundle {
    val external = new Bundle {
      val rxd = Input(Bool())
      val txd = Output(Bool())
    }
    val address = Flipped(Decoupled(UInt(2.W)))
    val readPort = Decoupled(UInt(8.W))
    val writePort = Flipped(Decoupled(UInt(8.W)))
  })

  io.readPort.bits := "h00".U
  io.address.ready := true.B
  io.readPort.valid := true.B
  io.writePort.ready := true.B

  val receiver = Module(new UARTReceiver)
  val transmitter = Module(new UARTTransmitter)
  receiver.io.rxd := io.external.rxd
  io.external.txd := transmitter.io.txd

  val rxAvailable = RegInit(false.B)
  val txBegin = RegInit(false.B)

  val rxData = RegInit(0.U(8.W))
  val txData = RegInit(0.U(8.W))

  transmitter.io.data.valid := txBegin
  transmitter.io.data.bits := txData
  when (txBegin) {
    txBegin := !transmitter.io.data.ready
  }

  receiver.io.data.ready := !rxAvailable
  when (!rxAvailable) {
    when (receiver.io.data.valid) {
      rxAvailable := true.B
      rxData := receiver.io.data.bits
    }
  }

  when (io.address.valid) {
    switch(io.address.bits) {
      is("b00".U) {
        io.readPort.bits := Cat(0.U(6.W), txBegin, rxAvailable)
        when(io.writePort.valid) {
          rxAvailable := io.writePort.bits(0)
          txBegin := io.writePort.bits(1)
        }
      }

      is("b01".U) {
        io.readPort.bits := rxData
      }

      is("b10".U) {
        when(io.writePort.valid) {
          txData := io.writePort.bits
        }
      }
    }
  }
}
