package uart

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object ReceiverStates extends ChiselEnum {
  val idle,
      startBit,
      dataBits,
      stopBit,
      done = Value
}

class UARTReceiver extends Module with UARTParameters {
  val io = IO(new Bundle {
    val rxd = Input(Bool())
    val data = Decoupled(UInt(8.W))
  })

  val state = RegInit(ReceiverStates.idle)

  val receivedByte = RegInit(VecInit(Seq.fill(8)(0.U(1.W))))
  val bitIndex = RegInit(0.U(log2Ceil(8).W))

  io.data.valid := false.B
  io.data.ready := DontCare
  io.data.bits := receivedByte.asUInt

  val rx1 = RegNext(io.rxd)
  val rx2 = RegNext(rx1)

  val counter = RegInit(0.U(log2Ceil(clockPerBit - 1).W))

  switch (state) {
    is (ReceiverStates.idle) {
      io.data.valid := false.B
      counter := 0.U
      bitIndex := 0.U

      when (rx2 === false.B) {
        state := ReceiverStates.startBit
      }
    }

    is (ReceiverStates.startBit) {
      when (counter === ((clockPerBit - 1) / 2).U) {
        when (rx2 === false.B) { // start signal still valid
          counter := 0.U
          state := ReceiverStates.dataBits
        }
      } otherwise {
        counter := counter + 1.U
      }
    }

    is (ReceiverStates.dataBits) {
      when (counter === (clockPerBit - 1).U) {
        counter := 0.U
        receivedByte(bitIndex) := rx2
        when (bitIndex < 7.U) {
          bitIndex := bitIndex + 1.U
        } otherwise {
          bitIndex := 0.U
          state := ReceiverStates.stopBit
        }
      } otherwise {
        counter := counter + 1.U
      }
    }

    is (ReceiverStates.stopBit) {
      when (counter === (clockPerBit - 1).U) {
        counter := 0.U
        io.data.valid := true.B
        state := ReceiverStates.done
      } otherwise {
        counter := counter + 1.U
      }
    }

    is (ReceiverStates.done) {
      io.data.valid := false.B
      state := ReceiverStates.idle
    }
  }
}
