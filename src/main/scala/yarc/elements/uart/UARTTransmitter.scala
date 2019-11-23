package uart

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object TransmitterStates extends ChiselEnum {
  val idle,
      startBit,
      dataBits,
      stopBit,
      done = Value
}

class UARTTransmitter extends Module with UARTParameters {
  val io = IO(new Bundle {
    val txd = Output(Bool())
    val data = Flipped(Decoupled(UInt(8.W)))
  })

  val state = RegInit(TransmitterStates.idle)

  val byteToTransmit = RegInit(0.U(8.W))
  val bitIndex = RegInit(0.U(log2Ceil(8).W))

  io.txd := true.B // high for idle

  io.data.ready := false.B

  val counter = RegInit(0.U(log2Ceil(clockPerBit - 1).W))

  switch (state) {
    is (TransmitterStates.idle) {
      io.data.ready := true.B
      counter := 0.U
      bitIndex := 0.U

      when (io.data.valid) {
        byteToTransmit := io.data.bits
        state := TransmitterStates.startBit
      }
    }

    is (TransmitterStates.startBit) {
      io.txd := false.B

      when (counter === (clockPerBit - 1).U) {
        counter := 0.U
        state := TransmitterStates.dataBits
      } otherwise {
        counter := counter + 1.U
      }
    }

    is (TransmitterStates.dataBits) {
      io.txd := byteToTransmit(bitIndex)
      when (counter === (clockPerBit - 1).U) {
        counter := 0.U
        when (bitIndex < 7.U) {
          bitIndex := bitIndex + 1.U
        } otherwise {
          bitIndex := 0.U
          state := TransmitterStates.stopBit
        }
      } otherwise {
        counter := counter + 1.U
      }
    }

    is (TransmitterStates.stopBit) {
      io.txd := true.B

      when (counter === (clockPerBit - 1).U) {
        counter := 0.U
        state := TransmitterStates.done
      } otherwise {
        counter := counter + 1.U
      }
    }

    is (TransmitterStates.done) {
      // do nothing currently, maybe trigger interrupt later
      state := TransmitterStates.idle
    }
  }
}
