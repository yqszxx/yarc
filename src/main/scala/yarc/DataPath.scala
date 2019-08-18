// See README.md for license details.

package yarc

import chisel3._
import yarc.elements._
import yarc.stages._

class PipelineIO extends Bundle {
  val done = Output(Bool())
}

class DataPath extends Module {
  val io = IO(new PipelineIO)

  // Register File
  val registerFile = Module(new RegisterFile)

  // Memory
  val memory = Module(new Memory)

  // Done logic
  val d = RegInit(false.B)
  val done = RegNext(d)
  when (memory.io.port2.address === "h0100".U && memory.io.port2.writeEnable) {
    printf(p"!!!!!!!!!!!!!!!!!!!DONE#0x${Hexadecimal(memory.io.port2.writeData)}#!!!!!!!!!!!!!!!!!!!\n")
    done := true.B
  } .otherwise {
    done := false.B
  }
  io.done := done


  // Fetch Stage
  val fetchStage = Module(new FetchStage)
  memory.io.port1 <> fetchStage.io.mem

  // Decode Stage
  val decodeStageInstruction = RegNext(fetchStage.io.data.instruction)
  val decodeStagePC = RegNext(fetchStage.io.data.pc)

  val decodeStage = Module(new DecodeStage)
  decodeStage.io.registers <> registerFile.io.readPort
  decodeStage.io.data.pc := decodeStagePC
  decodeStage.io.data.instruction := decodeStageInstruction

  printf(p"Decode PC: 0x${Hexadecimal(decodeStagePC)}\n")

  // Execute Stage
  val executeStageControlSignals = RegNext(decodeStage.io.control.executeStageControlSignals)
  val memoryStageControlSignalsInExecuteStage = RegNext(decodeStage.io.control.memoryStageControlSignals)
  val writebackStageControlSignalsInExecuteStage = RegNext(decodeStage.io.control.writebackStageControlSignals)
  val executeStagePC = RegNext(decodeStagePC)
  val executeStageWriteRegister = RegNext(decodeStage.io.data.writeRegister)
  val executeStageRegisterSource2 = RegNext(decodeStage.io.data.registerSource2)
  val executeStageOperator1 = RegNext(decodeStage.io.data.operator1)
  val executeStageOperator2 = RegNext(decodeStage.io.data.operator2)

  val executeStage = Module(new ExecuteStage)
  executeStage.io.control := executeStageControlSignals
  executeStage.io.data.operator1 := executeStageOperator1
  executeStage.io.data.operator2 := executeStageOperator2

  printf(p"Execute PC: 0x${Hexadecimal(executeStagePC)}\n")

  // Memory Stage
  val memoryStageControlSignals = RegNext(memoryStageControlSignalsInExecuteStage)
  val writebackStageControlSignalsInMemoryStage = RegNext(writebackStageControlSignalsInExecuteStage)
  val memoryStagePC = RegNext(executeStagePC)
  val memoryStageWriteRegister = RegNext(executeStageWriteRegister)
  val memoryStageALUResult = RegNext(executeStage.io.data.aluResult)
  val memoryStageRegisterSource2 = RegNext(executeStageRegisterSource2)

  val memoryStage = Module(new MemoryStage)
  memoryStage.io.control := memoryStageControlSignals
  memoryStage.io.mem <> memory.io.port2
  memoryStage.io.data.address := memoryStageALUResult
  memoryStage.io.data.writeData := memoryStageRegisterSource2

  printf(p"Memory PC: 0x${Hexadecimal(memoryStagePC)}\n")

  // Writeback Stage
  val writebackStageControlSignals = RegNext(writebackStageControlSignalsInMemoryStage)
  val writebackStagePC = RegNext(memoryStagePC)
  val writebackStageWriteRegister = RegNext(memoryStageWriteRegister)
  val writebackStageALUResult = RegNext(memoryStageALUResult)
  val writebackStageMemoryReadData = RegNext(memoryStage.io.data.readData)

  val writebackStage = Module(new WritebackStage)
  writebackStage.io.control := writebackStageControlSignals
  writebackStage.io.registers <> registerFile.io.writePort
  writebackStage.io.data.aluResult := writebackStageALUResult
  writebackStage.io.data.pc := writebackStagePC
  writebackStage.io.data.memoryReadData := writebackStageMemoryReadData
  writebackStage.io.data.writeRegister := writebackStageWriteRegister

  printf(p"Writeback PC: 0x${Hexadecimal(writebackStagePC)}\n")
}
