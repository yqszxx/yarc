// See README.md for license details.

package yarc

import chisel3._
import chisel3.util._
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

  // Print Logic
  when (memory.io.port2.address === "hFFF8".U && memory.io.port2.writeEnable) {
    printf(p"$$0x${Hexadecimal(memory.io.port2.writeData)}$$\n")
  }

  // Done logic
  val maxCycle = 10000

  val maxCycleU = maxCycle.U
  val cycle = RegInit(0.U(log2Ceil(maxCycle).W))
  cycle := cycle + 1.U
  printf(p"Cycle ${Decimal(cycle)}:\n")

  val done = RegInit(false.B)
  when (memory.io.port2.address(31, 24) === "hFF".U && memory.io.port2.writeEnable) {
    printf(p"!!!!!!!!!!!!!!!!!!!DONE#0x${Hexadecimal(memory.io.port2.writeData)}#!!!!!!!!!!!!!!!!!!!\n")
    done := true.B
  } .elsewhen(cycle === maxCycleU - 1.U) {
    printf(p"???????????????????DONE after ${Decimal(maxCycleU)} cycles???????????????????\n")
    done := true.B
  } .otherwise {
    done := false.B
  }
  io.done := done

  val bubblePC = "hFFFF".U(32.W)

  // Fetch Stage
  val fetchStage = Module(new FetchStage)
  memory.io.port1 <> fetchStage.io.mem

  // Decode Stage
  val decodeStageInstruction = Reg(UInt(32.W))
  val decodeStagePC = Reg(UInt(32.W))

  val decodeStage = Module(new DecodeStage)
  decodeStage.io.registers <> registerFile.io.readPort
  decodeStage.io.data.pc := decodeStagePC
  decodeStage.io.data.instruction := decodeStageInstruction

  fetchStage.io.kill := decodeStage.io.control.fetchStageKill // directly connect, not delayed by 1 cycle

  when (decodeStage.io.needStall) {
    decodeStageInstruction := decodeStageInstruction
    decodeStagePC := decodeStagePC
  } .otherwise {
    decodeStageInstruction := fetchStage.io.data.instruction
    decodeStagePC := fetchStage.io.data.pc
  }

  printf(p"Decode PC: 0x${Hexadecimal(decodeStagePC)}\n")

  // Execute Stage
  val fetchStageControlSignals = Reg(new FetchStageControlSignals)
  val executeStageControlSignals = Reg(new ExecuteStageControlSignals)
  val memoryStageControlSignalsInExecuteStage = Reg(new MemoryStageControlSignals)
  val writebackStageControlSignalsInExecuteStage = Reg(new WritebackStageControlSignals)
  val executeStagePC = Reg(UInt(32.W))
  val executeStageWriteRegister = Reg(UInt(5.W))
  val executeStageRegisterSource1 = Reg(UInt(32.W))
  val executeStageRegisterSource2 = Reg(UInt(32.W))
  val executeStageOperator1 = Reg(UInt(32.W))
  val executeStageOperator2 = Reg(UInt(32.W))

  // stall logic of this one resides in fetch stage
  fetchStageControlSignals := decodeStage.io.control.fetchStageControlSignals
  fetchStage.io.control := fetchStageControlSignals

  // Stall logic for execute stage
  when (decodeStage.io.needStall) { // decode stage is stalling, insert bubble here
    executeStageControlSignals.aluOperation := ALU.ALUOperation.notRelated
    memoryStageControlSignalsInExecuteStage.isWriting := MemoryStage.MemoryIsWriting.no
    writebackStageControlSignalsInExecuteStage.writebackSource := WritebackStage.WritebackSource.notRelated
    writebackStageControlSignalsInExecuteStage.isWriting := WritebackStage.RegisterIsWriting.no
    executeStagePC := bubblePC
    executeStageWriteRegister := 0.U(5.W)
    executeStageRegisterSource1 := 0.U(5.W)
    executeStageRegisterSource2 := 0.U(5.W)
    executeStageOperator1 := 0.U(32.W)
    executeStageOperator2 := 0.U(32.W)
  } .otherwise { // not stalling, use control signals generated by decode stage
    executeStageControlSignals := decodeStage.io.control.executeStageControlSignals
    memoryStageControlSignalsInExecuteStage := decodeStage.io.control.memoryStageControlSignals
    writebackStageControlSignalsInExecuteStage := decodeStage.io.control.writebackStageControlSignals
    executeStagePC := decodeStagePC
    executeStageWriteRegister := decodeStage.io.data.writeRegister
    executeStageRegisterSource1 := decodeStage.io.data.registerSource1
    executeStageRegisterSource2 := decodeStage.io.data.registerSource2
    executeStageOperator1 := decodeStage.io.data.operator1
    executeStageOperator2 := decodeStage.io.data.operator2
  }

  val executeStage = Module(new ExecuteStage)
  executeStage.io.control := executeStageControlSignals
  executeStage.io.data.operator1 := executeStageOperator1
  executeStage.io.data.operator2 := executeStageOperator2
  executeStage.io.data.registerSource1 := executeStageRegisterSource1
  executeStage.io.data.registerSource2 := executeStageRegisterSource2

  fetchStage.io.data.branchTarget := executeStage.io.data.aluResult // directly connect, not delayed by 1 cycle
  fetchStage.io.data.branchTaken := executeStage.io.data.branchTaken // directly connect, not delayed by 1 cycle

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

  // Write Register Feedback
  decodeStage.io.executeStageWriteRegister := executeStageWriteRegister
  decodeStage.io.memoryStageWriteRegister := memoryStageWriteRegister
  decodeStage.io.writebackStageWriteRegister := writebackStageWriteRegister

  // Stall logic
  fetchStage.io.stall := decodeStage.io.needStall
  printf(p"Stall: ${Binary(decodeStage.io.needStall)}\n")
}