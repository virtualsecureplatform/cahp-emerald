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

  val instADecoder = Module(new InstructionDecoder())
  val instBDecoder = Module(new InstructionDecoder())
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
    //printf("[ID] Instruction:0x%x\n", pIdRe
    //printf("[ID] Imm:0x%x\n", decoder.io.imm)
    //printf("[ID] RegWrite:0x%x\n", decoder.io.wbOut.regWrite)
  }
}
