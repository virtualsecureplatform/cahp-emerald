/*
Copyright 2019 Naoki Matsumoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import chisel3._
import chisel3.util.{Cat, Fill}

class IdUnitIn(implicit val conf:CAHPConfig) extends Bundle {
  val instA = Input(UInt(24.W))
  val instB = Input(UInt(24.W))
  val pc = Input(UInt(9.W))
}

class MainRegisterWritePortIn(implicit val conf:CAHPConfig) extends Bundle {
  val regWrite = Input(UInt(4.W))
  val regWriteData = Input(UInt(16.W))
  val regWriteEnable = Input(Bool())
}

class WbUnitIn(implicit val conf:CAHPConfig) extends Bundle {
  val instARegWrite = new MainRegisterWritePortIn()
  val instBRegWrite = new MainRegisterWritePortIn()

  val finishFlag = Input(Bool())
  val pc = Input(UInt(9.W))
}

class IdWbUnitPort (implicit val conf:CAHPConfig) extends Bundle {
  val idIn = new IdUnitIn
  val wbIn = new WbUnitIn
  val exMemIn = new MemUnitIn
  val exWbIn = new WbUnitIn
  val memWbIn = new WbUnitIn
  val idEnable = Input(Bool())
  val wbEnable = Input(Bool())
  val flush = Input(Bool())

  val exOut = Flipped(new ExUnitIn)
  val memOut = Flipped(new MemUnitIn)
  val wbOut = Flipped(new WbUnitIn)
  val stole = Output(Bool())

  val finishFlag = Output(Bool())
  val regOut = new MainRegisterOutPort()

  /*
  val debugRs = if (conf.test) Output(UInt(4.W)) else Output(UInt(0.W))
  val debugRd = if (conf.test) Output(UInt(4.W)) else Output(UInt(0.W))
  val debugRegWrite = if(conf.test) Output(Bool()) else Output(UInt(0.W))
  val debugImmLongState = if(conf.test) Output(Bool()) else Output(UInt(0.W))
  val testFinish = if (conf.test) Output(Bool()) else Output(UInt(0.W))
   */
  val testRegx8 = if (conf.test) Output(UInt(16.W)) else Output(UInt(0.W))
}

class DecoderPort(implicit val conf:CAHPConfig) extends Bundle {
  val inst = Input(UInt(24.W))
  val pc = Input(UInt(9.W))

  val imm = Output(UInt(16.W))
  val pcImm = Output(UInt(16.W))
  val pcImmSel = Output(Bool())
  val rs1 = Output(UInt(4.W))
  val rs2 = Output(UInt(4.W))
  val rd = Output(UInt(4.W))
  val longInst = Output(Bool())
  val inASel = Output(Bool())
  val inBSel = Output(Bool())
  val isJump = Output(Bool())
  val isMem = Output(Bool())
  val isFinish = Output(Bool())

  val exALUOut = Flipped(new ALUPortIn)
  val exBCOut = Flipped(new BranchControllerIn())
  val memOut = Flipped(new MemUnitIn)
  val wbOut = Flipped(new MainRegisterWritePortIn())

  val testImmType = if(conf.test) Output(UInt(4.W)) else Output(UInt(0.W))
  val testPCImmType = if(conf.test) Output(UInt(2.W)) else Output(UInt(0.W))
}

object InstructionCategory {
  def InstR:UInt = 0.U(2.W)
  def InstI:UInt = 1.U(2.W)
  def InstM:UInt = 2.U(2.W)
  def InstJ:UInt = 3.U(2.W)
}

object ImmType {
  def SImm10:UInt  = "b0001".U(4.W)
  def UImm7:UInt   = "b0010".U(4.W)
  def SImm6:UInt   = "b0100".U(4.W)
  def Imm2:UInt    = "b1000".U(4.W)
}

object PCImmType {
  def SImm10:UInt = "b01".U(2.W)
  def SImm11:UInt = "b10".U(2.W)
}

object PCOpcode {
  def SImm10:UInt = "b01".U(2.W)
  def SImm11:UInt = "b10".U(2.W)
}

object DecoderUtils {
  def getRegWrite(inst:UInt): Bool = {
    val regWrite = Wire(Bool())

    when(inst(2,1) === InstructionCategory.InstM){
      regWrite := inst(3) != 1.U
    }.elsewhen(inst(2,1) === InstructionCategory.InstJ) {
      regWrite := (inst(4) === 1.U) && (inst(0) === 0.U)
    }.elsewhen(inst(7,0) === 0.U){
      regWrite := false.B
    }.otherwise{
      regWrite := true.B
    }
    regWrite
  }

  def getRegRd(inst:UInt):UInt = {
    val rd = Wire(UInt(4.W))
    when(inst(2,1) === InstructionCategory.InstJ){
      rd := 0.U(4.W)
    }.otherwise{
      rd := inst(11, 8)
    }
    rd
  }
}

class Decoder(implicit val conf:CAHPConfig) extends Module {

  def genImm(inst:UInt, immType:UInt):UInt = {
    val imm = Wire(UInt(16.W))
    imm:=DontCare
    when(immType === ImmType.SImm10){
      imm := Cat(Fill(7, inst(7)), inst(6), inst(23, 16))
    }.elsewhen(immType === ImmType.UImm7){
      imm := Cat(0.U(9.W), inst(7, 6), inst(15, 12), 0.U(1.W))
    }.elsewhen(immType === ImmType.SImm6){
      imm := Cat(Fill(11, inst(7)), inst(6), inst(15, 12))
    }.elsewhen(immType === ImmType.Imm2){
      imm := 2.U(16.W)
    }
    imm
  }

  def genPCImm(inst:UInt, pcImmType:UInt):UInt = {
    val imm = Wire(UInt(16.W))
    imm:=DontCare
    when(pcImmType === PCImmType.SImm10){
      imm := Cat(Fill(7, inst(7)), inst(6), inst(23, 16))
    }.elsewhen(pcImmType === PCImmType.SImm11){
      imm := Cat(Fill(6, inst(15)), inst(15, 5))
    }
    imm
  }

  def getImmType(inst:UInt):UInt = {
    val immType = Wire(UInt(5.W))
    immType := DontCare
    when(inst(0) === 1.U) {
      immType := ImmType.SImm10
    }.otherwise{
      when(inst(2, 1) === InstructionCategory.InstM){
        when(inst(5, 4) === 1.U){
          immType := ImmType.UImm7
        }.otherwise{
          immType := ImmType.SImm6
        }
      }.elsewhen(inst(2, 1) === InstructionCategory.InstI){
        immType := ImmType.SImm6
      }.elsewhen(inst(2, 1) === InstructionCategory.InstJ){
        immType := ImmType.Imm2
      }
    }
    immType
  }

  def getPCImmType(inst:UInt):UInt =  {
    val pcImmType = Wire(UInt(2.W))
    pcImmType := DontCare
    when(inst(0) === 1.U) {
      pcImmType := PCImmType.SImm10
    }.otherwise{
      pcImmType := PCImmType.SImm11
    }
    pcImmType
  }

  def getPCImmSel(inst:UInt):UInt = {
    val pcImmSel = Wire(Bool())
    pcImmSel := true.B
    when(inst(0) === 0.U){
      when(inst(3) === 0.U) {
        pcImmSel := false.B
      }
    }
    pcImmSel
  }

  def getPCOpcode(inst:UInt):UInt = {
    val pcOpcode = Wire(UInt(3.W))
    pcOpcode := 0.U(3.W)
    when(inst(0) === 1.U){
      when(inst(2, 1) === InstructionCategory.InstJ){
        pcOpcode := inst(5, 3)
      }
    }.otherwise{
      when(inst(2, 1) === InstructionCategory.InstJ){
        pcOpcode := 4.U(3.W)
      }
    }
    pcOpcode
  }

  def getExOpcode(inst:UInt): UInt = {
    val exOpcode = Wire(UInt(4.W))
    exOpcode := DontCare
    when(inst(2,1) === InstructionCategory.InstR) {
      exOpcode := Cat(inst(6, 3))
    }.elsewhen(inst(2, 1) === InstructionCategory.InstI){
      exOpcode := Cat(0.U(1.W), inst(5, 3))
    }.elsewhen(inst(2,1) === InstructionCategory.InstM){
      when((inst(5,4) === 3.U)||(inst(5, 0) === 4.U)){
        exOpcode := 8.U(4.W)
      }.otherwise{
        exOpcode := 0.U(4.W)
      }
    }.otherwise{
      when(inst(0) === 1.U){
        exOpcode := 1.U(4.W)
      }.otherwise{
        exOpcode := 0.U(4.W)
      }
    }
    exOpcode
  }

  def getMemWrite(inst:UInt): Bool = {
    val memWrite = Wire(Bool())
    memWrite := DontCare
    when(inst(2,1) === InstructionCategory.InstM){
      memWrite := inst(3) === 1.U
    }.otherwise{
      memWrite := false.B
    }
    memWrite
  }

  def getMemRead(inst:UInt): Bool = {
    val memRead = Wire(Bool())
    memRead := false.B
    when(inst(2, 1) === InstructionCategory.InstM){
      when(inst(3) === 0.U){
        when(inst(5, 0) != "b110101".U(6.W) && inst(5, 0) != "b110100".U(6.W) && inst(5, 0) != "b100".U(6.W)){
          memRead := true.B
        }
      }
    }
    memRead
  }

  def getMemByte(inst:UInt): Bool ={
    val byteEnable = Wire(Bool())
    byteEnable := DontCare
    when(inst(2,1) === InstructionCategory.InstM){
      byteEnable := false.B
      when(inst(0) === 1.U){
        //LB, LBU, SB
        when(inst(5, 3) === 4.U(3.W) ||
          inst(5, 3) === 0.U(3.W) ||
          inst(5, 3) === 1.U(3.W)){
          byteEnable := true.B
        }
      }
    }
    byteEnable
  }

  def getMemSignExt(inst:UInt): Bool = {
    val signExt = Wire(Bool())
    signExt := DontCare
    when(inst(2,1) === InstructionCategory.InstM){
      signExt := false.B
      //LB
      when(inst(0) === 1.U && inst(5) === 1.U){
        signExt := true.B
      }
    }
    signExt
  }


  def getInASel(inst:UInt): Bool = {
    val inASel = Wire(Bool())
    val isLongInst:Bool = (inst(0) === 1.U)
    inASel := false.B
    when(!isLongInst){
      when(inst(2, 1) === InstructionCategory.InstJ){
        inASel := true.B
      }
    }
    inASel
  }

  def getInBSel(inst:UInt): Bool = {
    val inBSel = Wire(Bool())
    val isLongInst:Bool = (inst(0) === 1.U)
    when(inst(2, 1) === InstructionCategory.InstR){
      inBSel := false.B
    }.otherwise{
      inBSel := true.B
      when(isLongInst) {
        when(inst(2, 1) === InstructionCategory.InstJ){
          inBSel := false.B
        }
      }
    }
    inBSel
  }
  val io = IO(new DecoderPort)

  io.isJump := false.B
  io.isMem := false.B

  io.imm := genImm(io.inst, getImmType(io.inst))
  io.pcImm := genPCImm(io.inst, getPCImmType(io.inst))
  io.pcImmSel := getPCImmSel(io.inst)
  io.testImmType := getImmType(io.inst)
  io.testPCImmType := getPCImmType(io.inst)

  io.inASel := getInASel(io.inst)
  io.inBSel := getInBSel(io.inst)
  io.exALUOut.opcode := getExOpcode(io.inst)
  io.exALUOut.inA := DontCare
  io.exALUOut.inB := DontCare
  io.exBCOut.pcOpcode := getPCOpcode(io.inst)
  io.exBCOut.pcImm := DontCare
  io.exBCOut.pc := DontCare
  io.exBCOut.pcAdd := DontCare
  io.memOut.instAMemRead := getMemRead(io.inst)
  io.memOut.instBMemRead := DontCare
  io.memOut.memWrite := getMemWrite(io.inst)
  io.memOut.byteEnable := getMemByte(io.inst)
  io.memOut.signExt := getMemSignExt(io.inst)
  io.memOut.address := DontCare
  io.memOut.in := DontCare

  io.isMem := io.memOut.instAMemRead || io.memOut.memWrite

  io.longInst := (io.inst(0) === 1.U)

  when(io.longInst) {
    io.rs1 := io.inst(15, 12)
    when(io.inst(2, 1) === InstructionCategory.InstM || io.inst(2, 1) === InstructionCategory.InstJ){
      io.rs2 := io.inst(11, 8)
    }.otherwise{
      io.rs2 := io.inst(19,16)
    }
  }.otherwise{
    when(io.inst(2, 1) === InstructionCategory.InstM){
      io.rs1 := 1.U(4.W)
      io.rs2 := io.inst(11, 8)
    }.otherwise{
      io.rs1 := io.inst(11, 8)
      io.rs2 := io.inst(15, 12)
    }
  }
  when(io.inst(2,1) === InstructionCategory.InstJ){
    io.rd := 0.U(4.W)
    io.isJump := true.B
  }.otherwise{
    io.rd := io.inst(11, 8)
  }

  io.wbOut.regWrite := io.rd
  io.wbOut.regWriteEnable := DecoderUtils.getRegWrite(io.inst)
  io.wbOut.regWriteData := DontCare
  io.isFinish := (io.inst(15,0) === 0xE.U)
}

class ForwardController(implicit val conf:CAHPConfig) extends Module{
  val io = IO(new Bundle{
    val rs = Input(UInt(4.W))
    val rsData = Input(UInt(16.W))
    val instAEx = new MainRegisterWritePortIn()
    val instBEx = new MainRegisterWritePortIn()
    val instAMem = new MainRegisterWritePortIn()
    val instBMem = new MainRegisterWritePortIn()
    val rsDataOut = Output(UInt(16.W))
  })
  when(io.rs === io.instAEx.regWrite && io.instAEx.regWriteEnable) {
    //Forward from EX
    io.rsDataOut := io.instAEx.regWriteData
  }.elsewhen(io.rs === io.instBEx.regWrite && io.instBEx.regWriteEnable) {
    //Forward from EX
    io.rsDataOut := io.instBEx.regWriteData
  }.elsewhen(io.rs === io.instAMem.regWrite && io.instAMem.regWriteEnable) {
    //Forward from MEM
    io.rsDataOut := io.instAMem.regWriteData
  }.elsewhen(io.rs === io.instBMem.regWrite && io.instBMem.regWriteEnable){
    //Forward from MEM
    io.rsDataOut := io.instBMem.regWriteData
  }.otherwise{
    io.rsDataOut := io.rsData
  }
}

class IdWbUnit(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new IdWbUnitPort)

  val instADecoder = Module(new Decoder())
  val instBDecoder = Module(new Decoder())
  val mainRegister = Module(new MainRegister())

  val instARs1Fwd = Module(new ForwardController())
  val instARs2Fwd = Module(new ForwardController())

  val instBRs1Fwd = Module(new ForwardController())
  val instBRs2Fwd = Module(new ForwardController())

  val pIdReg = RegInit(0.U.asTypeOf(new IdUnitIn))

  val stole = Wire(Bool())
  stole := false.B

  when(io.idEnable&&io.flush){
    pIdReg := io.idIn
    pIdReg.instA := 0.U(16.W)
    pIdReg.instB := 0.U(16.W)
  }.elsewhen(io.idEnable&&(!stole)){
    pIdReg := io.idIn
  }

  instADecoder.io.inst := pIdReg.instA
  instBDecoder.io.inst := pIdReg.instB

  mainRegister.io.portA.rs1 := instADecoder.io.rs1
  mainRegister.io.portA.rs2 := instADecoder.io.rs2
  mainRegister.io.portB.rs1 := instBDecoder.io.rs1
  mainRegister.io.portB.rs2 := instBDecoder.io.rs2

  mainRegister.io.portA.rd := io.wbIn.instARegWrite.regWrite
  mainRegister.io.portA.writeData := io.wbIn.instARegWrite.regWriteData
  mainRegister.io.portA.writeEnable := io.wbIn.instARegWrite.regWriteEnable
  mainRegister.io.portB.rd := io.wbIn.instBRegWrite.regWrite
  mainRegister.io.portB.writeData := io.wbIn.instBRegWrite.regWriteData
  mainRegister.io.portB.writeEnable := io.wbIn.instBRegWrite.regWriteEnable
  //mainRegister.io.testPC := io.wbIn.pc

  //io.exOut. := decoder.io.exOut
  //io.exOut.pc := pIdReg.pc
  io.exOut.instAALU := instADecoder.io.exALUOut
  io.exOut.instBALU := instBDecoder.io.exALUOut

  instARs1Fwd.io.rs := instADecoder.io.rs1
  instARs1Fwd.io.rsData := mainRegister.io.portA.rs1Data
  instARs1Fwd.io.instAEx := io.exWbIn.instARegWrite
  instARs1Fwd.io.instBEx := io.exWbIn.instBRegWrite
  instARs1Fwd.io.instAMem := io.memWbIn.instARegWrite
  instARs1Fwd.io.instBMem := io.memWbIn.instBRegWrite

  instARs2Fwd.io.rs := instADecoder.io.rs2
  instARs2Fwd.io.rsData := mainRegister.io.portA.rs2Data
  instARs2Fwd.io.instAEx := io.exWbIn.instARegWrite
  instARs2Fwd.io.instBEx := io.exWbIn.instBRegWrite
  instARs2Fwd.io.instAMem := io.memWbIn.instARegWrite
  instARs2Fwd.io.instBMem := io.memWbIn.instBRegWrite

  instBRs1Fwd.io.rs := instBDecoder.io.rs1
  instBRs1Fwd.io.rsData := mainRegister.io.portB.rs1Data
  instBRs1Fwd.io.instAEx := io.exWbIn.instARegWrite
  instBRs1Fwd.io.instBEx := io.exWbIn.instBRegWrite
  instBRs1Fwd.io.instAMem := io.memWbIn.instARegWrite
  instBRs1Fwd.io.instBMem := io.memWbIn.instBRegWrite

  instBRs2Fwd.io.rs := instBDecoder.io.rs2
  instBRs2Fwd.io.rsData := mainRegister.io.portB.rs2Data
  instBRs2Fwd.io.instAEx := io.exWbIn.instARegWrite
  instBRs2Fwd.io.instBEx := io.exWbIn.instBRegWrite
  instBRs2Fwd.io.instAMem := io.memWbIn.instARegWrite
  instBRs2Fwd.io.instBMem := io.memWbIn.instBRegWrite

  when(
      (instADecoder.io.rs1 === io.exWbIn.instARegWrite.regWrite)||
      (instADecoder.io.rs2 === io.exWbIn.instARegWrite.regWrite)||
      (instBDecoder.io.rs1 === io.exWbIn.instARegWrite.regWrite)||
      (instBDecoder.io.rs2 === io.exWbIn.instARegWrite.regWrite)
  ){
    stole := stole || io.exMemIn.instAMemRead
  }

  when(
    (instADecoder.io.rs1 === io.exWbIn.instBRegWrite.regWrite)||
      (instADecoder.io.rs2 === io.exWbIn.instBRegWrite.regWrite)||
      (instBDecoder.io.rs1 === io.exWbIn.instBRegWrite.regWrite)||
      (instBDecoder.io.rs2 === io.exWbIn.instBRegWrite.regWrite)
  ){
    stole := stole || io.exMemIn.instBMemRead
  }

  //Select Branch Address Source
  when(instADecoder.io.isJump){
    when(instADecoder.io.pcImmSel){
      io.exOut.bcIn.pcImm := instADecoder.io.pcImm
      io.exOut.bcIn.pcAdd := true.B
    }.otherwise{
      io.exOut.bcIn.pcImm := instARs1Fwd.io.rsDataOut
      io.exOut.bcIn.pcAdd := false.B
    }
    io.exOut.bcIn.pcOpcode := instADecoder.io.exBCOut.pcOpcode
  }.otherwise{
    when(instBDecoder.io.pcImmSel){
      io.exOut.bcIn.pcImm := instBDecoder.io.pcImm
      io.exOut.bcIn.pcAdd := true.B
    }.otherwise{
      io.exOut.bcIn.pcImm := instBRs1Fwd.io.rsDataOut
      io.exOut.bcIn.pcAdd := false.B
    }
    io.exOut.bcIn.pcOpcode := instBDecoder.io.exBCOut.pcOpcode
  }
  io.exOut.bcIn.pc := pIdReg.pc

  when(!instADecoder.io.inASel){
    io.exOut.instAALU.inA := instARs1Fwd.io.rsDataOut
  }.otherwise{
    io.exOut.instAALU.inA := pIdReg.pc
  }
  when(!instADecoder.io.inBSel){
    io.exOut.instAALU.inB := instARs2Fwd.io.rsDataOut
  }.otherwise{
    //LUI
    when(pIdReg.instA(5, 0) === "b000100".U(6.W)){
      io.exOut.instAALU.inB := Cat(instADecoder.io.imm(5, 0), 0.U(10.W))
    }.otherwise{
      io.exOut.instAALU.inB := instADecoder.io.imm
    }
  }

  when(!instBDecoder.io.inASel) {
    io.exOut.instBALU.inA := instBRs1Fwd.io.rsDataOut
  }.otherwise{
    io.exOut.instBALU.inA := pIdReg.pc
  }
  when(!instBDecoder.io.inBSel){
    io.exOut.instBALU.inB := instBRs2Fwd.io.rsDataOut
  }.otherwise{
    //LUI
    when(pIdReg.instB(5, 0) === "b000100".U(6.W)){
      io.exOut.instBALU.inB := Cat(instBDecoder.io.imm(5, 0), 0.U(10.W))
    }.otherwise{
      io.exOut.instBALU.inB := instBDecoder.io.imm
    }
  }

  when(instADecoder.io.isMem){
    io.memOut := instADecoder.io.memOut
    io.memOut.in := instARs2Fwd.io.rsDataOut
  }.otherwise{
    io.memOut := instBDecoder.io.memOut
    io.memOut.in := instBRs2Fwd.io.rsDataOut
  }
  io.memOut.instAMemRead := instADecoder.io.memOut.instAMemRead
  io.memOut.instBMemRead := instBDecoder.io.memOut.instAMemRead

  io.wbOut.instARegWrite := instADecoder.io.wbOut
  io.wbOut.instBRegWrite := instBDecoder.io.wbOut
  io.wbOut.finishFlag := instADecoder.io.isFinish | instBDecoder.io.isFinish
  io.wbOut.pc := pIdReg.pc

  io.stole := stole
  io.regOut := mainRegister.io.regOut

  val finishFlagReg = RegInit(false.B)
  when(io.wbIn.finishFlag){
    finishFlagReg := io.wbIn.finishFlag
  }.otherwise{
    finishFlagReg := finishFlagReg
  }
  io.finishFlag := finishFlagReg

  when(stole){
    io.memOut.memWrite := false.B
    io.memOut.instAMemRead := false.B
    io.memOut.instBMemRead := false.B
    io.wbOut.instARegWrite.regWriteEnable := false.B
    io.wbOut.instBRegWrite.regWriteEnable := false.B
    io.wbOut.finishFlag := false.B
    io.exOut.bcIn.pcOpcode := 0.U
  }

  //io.testRegx8 := mainRegister.io.testRegx8
  io.wbOut.pc := io.idIn.pc
  when(conf.debugId.B){
    printf("[ID] PC Address:0x%x\n", pIdReg.pc)
    //printf("[ID] Instruction:0x%x\n", pIdReg.inst)
    //printf("[ID] Imm:0x%x\n", decoder.io.imm)
    //printf("[ID] RegWrite:0x%x\n", decoder.io.wbOut.regWrite)
  }
}
