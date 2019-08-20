// See README.md for license details.

package yarc.stages

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import yarc.elements.ImmediateNumberGenerator

object Instructions {
  def BEQ                = BitPat("b?????????????????000?????1100011")
  def BNE                = BitPat("b?????????????????001?????1100011")
  def BLT                = BitPat("b?????????????????100?????1100011")
  def BGE                = BitPat("b?????????????????101?????1100011")
  def BLTU               = BitPat("b?????????????????110?????1100011")
  def BGEU               = BitPat("b?????????????????111?????1100011")
  def JALR               = BitPat("b?????????????????000?????1100111")
  def JAL                = BitPat("b?????????????????????????1101111")
  def LUI                = BitPat("b?????????????????????????0110111")
  def AUIPC              = BitPat("b?????????????????????????0010111")
  def ADDI               = BitPat("b?????????????????000?????0010011")
  def SLLI               = BitPat("b000000???????????001?????0010011")
  def SLTI               = BitPat("b?????????????????010?????0010011")
  def SLTIU              = BitPat("b?????????????????011?????0010011")
  def XORI               = BitPat("b?????????????????100?????0010011")
  def SRLI               = BitPat("b000000???????????101?????0010011")
  def SRAI               = BitPat("b010000???????????101?????0010011")
  def ORI                = BitPat("b?????????????????110?????0010011")
  def ANDI               = BitPat("b?????????????????111?????0010011")
  def ADD                = BitPat("b0000000??????????000?????0110011")
  def SUB                = BitPat("b0100000??????????000?????0110011")
  def SLL                = BitPat("b0000000??????????001?????0110011")
  def SLT                = BitPat("b0000000??????????010?????0110011")
  def SLTU               = BitPat("b0000000??????????011?????0110011")
  def XOR                = BitPat("b0000000??????????100?????0110011")
  def SRL                = BitPat("b0000000??????????101?????0110011")
  def SRA                = BitPat("b0100000??????????101?????0110011")
  def OR                 = BitPat("b0000000??????????110?????0110011")
  def AND                = BitPat("b0000000??????????111?????0110011")
  def ADDIW              = BitPat("b?????????????????000?????0011011")
  def SLLIW              = BitPat("b0000000??????????001?????0011011")
  def SRLIW              = BitPat("b0000000??????????101?????0011011")
  def SRAIW              = BitPat("b0100000??????????101?????0011011")
  def ADDW               = BitPat("b0000000??????????000?????0111011")
  def SUBW               = BitPat("b0100000??????????000?????0111011")
  def SLLW               = BitPat("b0000000??????????001?????0111011")
  def SRLW               = BitPat("b0000000??????????101?????0111011")
  def SRAW               = BitPat("b0100000??????????101?????0111011")
  def LB                 = BitPat("b?????????????????000?????0000011")
  def LH                 = BitPat("b?????????????????001?????0000011")
  def LW                 = BitPat("b?????????????????010?????0000011")
  def LD                 = BitPat("b?????????????????011?????0000011")
  def LBU                = BitPat("b?????????????????100?????0000011")
  def LHU                = BitPat("b?????????????????101?????0000011")
  def LWU                = BitPat("b?????????????????110?????0000011")
  def SB                 = BitPat("b?????????????????000?????0100011")
  def SH                 = BitPat("b?????????????????001?????0100011")
  def SW                 = BitPat("b?????????????????010?????0100011")
  def SD                 = BitPat("b?????????????????011?????0100011")
//  def FENCE              = BitPat("b?????????????????000?????0001111")
//  def FENCE_I            = BitPat("b?????????????????001?????0001111")
//  def ECALL              = BitPat("b00000000000000000000000001110011")
//  def EBREAK             = BitPat("b00000000000100000000000001110011")
//  def MRET               = BitPat("b00110000001000000000000001110011")
//  def DRET               = BitPat("b01111011001000000000000001110011")
//  def SFENCE_VMA         = BitPat("b0001001??????????000000001110011")
//  def WFI                = BitPat("b00010000010100000000000001110011")
//  def CSRRW              = BitPat("b?????????????????001?????1110011")
//  def CSRRS              = BitPat("b?????????????????010?????1110011")
//  def CSRRC              = BitPat("b?????????????????011?????1110011")
//  def CSRRWI             = BitPat("b?????????????????101?????1110011")
//  def CSRRSI             = BitPat("b?????????????????110?????1110011")
//  def CSRRCI             = BitPat("b?????????????????111?????1110011")
}

object DecodeStage {
  object Operator1Source extends ChiselEnum  {
    val registerSource1,
        pc = Value
    val notRelated = registerSource1
  }
  object Operator2Source extends ChiselEnum  {
    val registerSource2,
        immediateNumber = Value
    val notRelated = registerSource2
  }
}

class DecodeStageIO extends Bundle {
  val control = new Bundle {
    val executeStageControlSignals = Output(new ExecuteStageControlSignals)
    val memoryStageControlSignals = Output(new MemoryStageControlSignals)
    val writebackStageControlSignals = Output(new WritebackStageControlSignals)
  }

  val data = new Bundle {
    val pc              = Input(UInt(32.W))
    val instruction     = Input(UInt(32.W))
    val operator1       = Output(UInt(32.W))
    val operator2       = Output(UInt(32.W))
    val writeRegister   = Output(UInt(5.W)) // TODO: find a better way to express register number
    val registerSource2 = Output(UInt(32.W))
  }

  val needStall = Output(Bool())
  val executeStageWriteRegister = Input(UInt(5.W))
  val memoryStageWriteRegister = Input(UInt(5.W))
  val writebackStageWriteRegister = Input(UInt(5.W))

  import yarc.elements.RegisterFileReadPort
  val registers = Flipped(new RegisterFileReadPort)
}

class DecodeStage extends Module {
  val io = IO(new DecodeStageIO)

  import Instructions._
  import DecodeStage.{Operator1Source, Operator2Source}
  import MemoryStage.MemoryIsWriting
  import WritebackStage.{RegisterIsWriting, WritebackSource}
  import yarc.elements.ALU.ALUOperation
  import yarc.elements.ImmediateNumberGenerator.ImmediateNumberType
  val controlSignals = ListLookup(io.data.instruction,
                List(Operator1Source.notRelated,      Operator2Source.notRelated,      ImmediateNumberType.notRelated, ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.no, WritebackSource.notRelated),
    Array(
      LW     -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.memoryReadData),
      LB     -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.memoryReadData),
      LBU    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.memoryReadData),
      LH     -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.memoryReadData),
      LHU    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.memoryReadData),
      SW     -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.sType,      ALUOperation.add,        MemoryIsWriting.yes, RegisterIsWriting.no,  WritebackSource.notRelated),
      SB     -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.sType,      ALUOperation.add,        MemoryIsWriting.yes, RegisterIsWriting.no,  WritebackSource.notRelated),
      SH     -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.sType,      ALUOperation.add,        MemoryIsWriting.yes, RegisterIsWriting.no,  WritebackSource.notRelated),

      AUIPC  -> List(Operator1Source.pc             , Operator2Source.immediateNumber, ImmediateNumberType.uType,      ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      LUI    -> List(Operator1Source.notRelated     , Operator2Source.immediateNumber, ImmediateNumberType.uType,      ALUOperation.copy2,      MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),

      ADDI   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      ANDI   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.and,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      ORI    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.or,         MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      XORI   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.xor,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SLTI   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.slt,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SLTIU  -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.sltu,       MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SLLI   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.sll,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SRAI   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.sra,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SRLI   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.srl,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),

      SLL    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.sll,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      ADD    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.add,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SUB    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.sub,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SLT    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.slt,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SLTU   -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.sltu,       MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      AND    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.and,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      OR     -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.or,         MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      XOR    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.xor,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SRA    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.sra,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),
      SRL    -> List(Operator1Source.registerSource1, Operator2Source.registerSource2, ImmediateNumberType.notRelated, ALUOperation.srl,        MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.aluResult),

      JAL    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.jType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.pcPlus4),
      JALR   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.iType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.yes, WritebackSource.pcPlus4),

      BEQ    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.bType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.no,  WritebackSource.notRelated),
      BNE    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.bType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.no,  WritebackSource.notRelated),
      BGE    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.bType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.no,  WritebackSource.notRelated),
      BGEU   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.bType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.no,  WritebackSource.notRelated),
      BLT    -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.bType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.no,  WritebackSource.notRelated),
      BLTU   -> List(Operator1Source.registerSource1, Operator2Source.immediateNumber, ImmediateNumberType.bType,      ALUOperation.notRelated, MemoryIsWriting.no,  RegisterIsWriting.no,  WritebackSource.notRelated)
    )
  )
  val operator1Source :: operator2Source :: immediateType :: aluOperation :: memoryIsWriting :: registerIsWriting :: writebackSource :: Nil = controlSignals
  // exported control signals
  io.control.executeStageControlSignals.aluOperation := aluOperation
  io.control.memoryStageControlSignals.isWriting := memoryIsWriting
  io.control.writebackStageControlSignals.isWriting := registerIsWriting
  io.control.writebackStageControlSignals.writebackSource := writebackSource
  // exported data signals
  io.data.writeRegister := io.data.instruction(11, 7)

  // registers
  val readRegister1 = io.data.instruction(19, 15)
  val readRegister2 = io.data.instruction(24, 20)
  io.registers.readRegister1 := readRegister1
  io.registers.readRegister2 := readRegister2
  io.data.registerSource2 := io.registers.readData2

  // operator 1 mux
  io.data.operator1 := MuxLookup(
    operator1Source.asUInt,
    io.registers.readData1,
    Array(
      Operator1Source.registerSource1.asUInt -> io.registers.readData1,
      Operator1Source.pc.asUInt -> io.data.pc
    )
  )

  val immediateNumberGenerator = Module(new ImmediateNumberGenerator)
  immediateNumberGenerator.io.instruction := io.data.instruction
  immediateNumberGenerator.io.immediateType := immediateType

  // operator 2 mux
  io.data.operator2 := MuxLookup(
    operator2Source.asUInt,
    io.registers.readData2,
    Array(
      Operator2Source.registerSource2.asUInt -> io.registers.readData2,
      Operator2Source.immediateNumber.asUInt -> immediateNumberGenerator.io.immediate.asUInt
    )
  )

  // Stall logic
  when (
    (operator1Source === Operator1Source.registerSource1 && readRegister1 =/= 0.U(5.W) && (
      readRegister1 === io.executeStageWriteRegister ||
      readRegister1 === io.memoryStageWriteRegister ||
      readRegister1 === io.writebackStageWriteRegister
    )) ||
    ((operator2Source === Operator2Source.registerSource2 || memoryIsWriting === MemoryIsWriting.yes) && readRegister2 =/= 0.U(5.W) &&  (
      readRegister2 === io.executeStageWriteRegister ||
      readRegister2 === io.memoryStageWriteRegister ||
      readRegister2 === io.writebackStageWriteRegister
    ))
  ) {
    io.needStall := true.B
  } .otherwise {
    io.needStall := false.B
  }

  printf(p"Decoding: ${Hexadecimal(io.data.instruction)}\n")
}
