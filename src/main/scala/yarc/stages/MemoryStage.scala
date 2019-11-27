// See README.md for license details.

package yarc.stages

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum


object MemoryStage {
  object MemoryOperationWidth extends ChiselEnum {
    val word,
        halfWord,
        byte,
        notRelated = Value
  }
  object MemoryExtendType extends ChiselEnum {
    val signExtend,
        zeroExtend = Value
  }
  object MemoryIsWriting extends ChiselEnum {
    val no  = Value(false.B)
    val yes = Value(true.B)
  }
}

class MemoryStageControlSignals extends Bundle {
  import MemoryStage._
  val isWriting = MemoryIsWriting()
  val operationWidth = MemoryOperationWidth()
  val extendType = MemoryExtendType()
}

class MemoryStageIO extends Bundle {
  val control = Input(new MemoryStageControlSignals)

  val data = new Bundle {
    val writeData = Input(UInt(32.W))
    val address   = Input(UInt(32.W))
    val readData  = Output(UInt(32.W))
  }

  import yarc.elements.MemoryReadWritePort
  val mem = Flipped(new MemoryReadWritePort)
}

class MemoryStage extends Module {
  import MemoryStage._

  val io = IO(new MemoryStageIO)

  val isWriting = io.control.isWriting.asUInt.asBool
  val operationWidth = io.control.operationWidth
  val extendType = io.control.extendType
  val writeMask = io.mem.writeMask
  val writeDataOut = io.mem.writeData
  val writeDataIn = io.data.writeData
  val readDataIn = io.mem.readData
  val readDataOut = io.data.readData
  io.mem.address := Cat(io.data.address(31, 2), 0.U(2.W))
  writeDataOut := "hDDEEAADD".U
  io.mem.writeEnable := isWriting
  readDataOut := "hBBEEEEFF".U

  val maskZero     = VecInit(Seq(false.B, false.B, false.B, false.B))
  val maskAll      = VecInit(Seq(true.B,  true.B,  true.B,  true.B ))
  val maskHighHalf = VecInit(Seq(false.B, false.B, true.B,  true.B ))
  val maskLowHalf  = VecInit(Seq(true.B,  true.B,  false.B, false.B))
  val mask1stByte  = VecInit(Seq(true.B,  false.B, false.B, false.B))
  val mask2ndByte  = VecInit(Seq(false.B, true.B,  false.B, false.B))
  val mask3rdByte  = VecInit(Seq(false.B, false.B, true.B,  false.B))
  val mask4thByte  = VecInit(Seq(false.B, false.B, false.B, true.B ))
  writeMask := maskZero

  when (isWriting) {
    switch (operationWidth) {
      is (MemoryOperationWidth.word) {
        writeMask := maskAll
        writeDataOut := writeDataIn
      }
      is (MemoryOperationWidth.halfWord) {
        switch (io.data.address(1).asUInt) {
          is ("b0".U) {
            writeMask := maskLowHalf
            writeDataOut := Cat(0.U(16.W), writeDataIn(15, 0))
          }
          is ("b1".U) {
            writeMask := maskHighHalf
            writeDataOut := Cat(writeDataIn(15, 0), 0.U(16.W))
          }
        }
      }
      is (MemoryOperationWidth.byte) {
        switch (io.data.address(1, 0)) {
          is("b00".U) {
            writeMask := mask1stByte
            writeDataOut := Cat(0.U(24.W), writeDataIn(7, 0))
          }
          is("b01".U) {
            writeMask := mask2ndByte
            writeDataOut := Cat(0.U(16.W), writeDataIn(7, 0), 0.U(8.W))
          }
          is("b10".U) {
            writeMask := mask3rdByte
            writeDataOut := Cat(0.U(8.W), writeDataIn(7, 0), 0.U(16.W))
          }
          is("b11".U) {
            writeMask := mask4thByte
            writeDataOut := Cat(writeDataIn(7, 0), 0.U(24.W))
          }
        }
      }
    }
  } otherwise {
    switch (operationWidth) {
      is (MemoryOperationWidth.word) {
        readDataOut := readDataIn
      }
      is (MemoryOperationWidth.halfWord) {
        switch (io.data.address(1).asUInt) {
          is ("b0".U) {
            switch (extendType) {
              is (MemoryExtendType.signExtend) {
                readDataOut := Cat(Fill(16, readDataIn(15)), readDataIn(15, 0))
              }
              is (MemoryExtendType.zeroExtend) {
                readDataOut := Cat(0.U(16.W), readDataIn(15, 0))
              }
            }
          }
          is ("b1".U) {
            switch (extendType) {
              is (MemoryExtendType.signExtend) {
                readDataOut := Cat(Fill(16, readDataIn(31)), readDataIn(31, 16))
              }
              is (MemoryExtendType.zeroExtend) {
                readDataOut := Cat(0.U(16.W), readDataIn(31, 16))
              }
            }
          }
        }
      }
      is (MemoryOperationWidth.byte) {
        switch(io.data.address(1, 0)) {
          is("b00".U) {
            switch (extendType) {
              is (MemoryExtendType.signExtend) {
                readDataOut := Cat(Fill(24, readDataIn(7)), readDataIn(7, 0))
              }
              is (MemoryExtendType.zeroExtend) {
                readDataOut := Cat(0.U(24.W), readDataIn(7, 0))
              }
            }
          }
          is("b01".U) {
            switch (extendType) {
              is (MemoryExtendType.signExtend) {
                readDataOut := Cat(Fill(24, readDataIn(15)), readDataIn(15, 8))
              }
              is (MemoryExtendType.zeroExtend) {
                readDataOut := Cat(0.U(24.W), readDataIn(15, 8))
              }
            }
          }
          is("b10".U) {
            switch (extendType) {
              is (MemoryExtendType.signExtend) {
                readDataOut := Cat(Fill(24, readDataIn(23)), readDataIn(23, 16))
              }
              is (MemoryExtendType.zeroExtend) {
                readDataOut := Cat(0.U(24.W), readDataIn(23, 16))
              }
            }
          }
          is("b11".U) {
            switch (extendType) {
              is (MemoryExtendType.signExtend) {
                readDataOut := Cat(Fill(24, readDataIn(31)), readDataIn(31, 24))
              }
              is (MemoryExtendType.zeroExtend) {
                readDataOut := Cat(0.U(24.W), readDataIn(31, 24))
              }
            }
          }
        }
      }
    }
  }
}
