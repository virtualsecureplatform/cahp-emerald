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
import chisel3.util.{BitPat, Cat}

class ALUPort(implicit val conf:CAHPConfig) extends Bundle{
  val inA = Input(UInt(16.W))
  val inB = Input(UInt(16.W))
  val opcode = Input(UInt(4.W))

  val out = Output(UInt(16.W))
  val flagCarry = Output(Bool())
  val flagOverflow = Output(Bool())
  val flagSign = Output(Bool())
  val flagZero = Output(Bool())
}

class ALU(implicit val conf:CAHPConfig) extends Module {

  def check_overflow(s1: UInt, s2: UInt, r: UInt) = {
    val s1_sign = Wire(UInt(1.W))
    val s2_sign = Wire(UInt(1.W))
    val res_sign = Wire(UInt(1.W))
    val res = Wire(Bool())
    s1_sign := s1(15)
    s2_sign := s2(15)
    res_sign := r(15)
    when(((s1_sign ^ s2_sign) === 0.U) && ((s2_sign ^ res_sign) === 1.U)) {
      res := true.B
    }.otherwise {
      res := false.B
    }
    res
  }

  val io = IO(new ALUPort)
  val resCarry = Wire(UInt(17.W))
  val inB_sub = Wire(UInt(16.W))
  resCarry := DontCare
  inB_sub := (~io.inB).asUInt()+1.U

  when(io.opcode === ALUOpcode.ADD) {
    io.out := io.inA + io.inB
  }.elsewhen(io.opcode === ALUOpcode.SUB) {
    resCarry := io.inA +& inB_sub
    io.out := resCarry(15, 0)
  }.elsewhen(io.opcode === ALUOpcode.AND) {
    io.out := io.inA & io.inB
  }.elsewhen(io.opcode === ALUOpcode.OR) {
    io.out := io.inA | io.inB
  }.elsewhen(io.opcode === ALUOpcode.XOR) {
    io.out := io.inA ^ io.inB
  }.elsewhen(io.opcode === ALUOpcode.LSL) {
    io.out := (io.inA << io.inB).asUInt()
  }.elsewhen(io.opcode === ALUOpcode.LSR) {
    io.out := (io.inA >> io.inB).asUInt()
  }.elsewhen(io.opcode === ALUOpcode.ASR) {
    io.out := (io.inA.asSInt() >> io.inB).asUInt()
  }.elsewhen(io.opcode === ALUOpcode.MOV) {
    io.out := io.inB
  }.otherwise {
    io.out := DontCare
  }

  io.flagCarry := ~resCarry(16)
  io.flagSign := io.out(15)
  io.flagZero := (io.out === 0.U(16.W))
  io.flagOverflow := check_overflow(io.inA, inB_sub, io.out)
}

class ExUnitPort(implicit val conf:CAHPConfig) extends Bundle {
  val in = new ExUnitIn
  val memIn = new MemUnitIn
  val wbIn = new WbUnitIn
  val enable = Input(Bool())
  val flush = Input(Bool())

  val out = new ExUnitOut
  val memOut = Flipped(new MemUnitIn)
  val wbOut = Flipped(new WbUnitIn)
}
class ExUnitIn extends Bundle {
  val inA = Input(UInt(16.W))
  val inB = Input(UInt(16.W))
  val opcode = Input(UInt(4.W))

  val pcOpcode = Input(UInt(3.W))
  val pc = Input(UInt(16.W))
  val pcImm = Input(UInt(16.W))
  val pcAdd = Input(Bool())
}

class ExUnitOut(implicit val conf:CAHPConfig) extends Bundle {
  val res = Output(UInt(16.W))
  val jumpAddress = Output(UInt(conf.romAddrWidth.W))
  val jump = Output(Bool())
}

class ExUnit(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new ExUnitPort)
  val alu = Module(new ALU)
  val pExReg = RegInit(0.U.asTypeOf(new ExUnitIn))
  val pMemReg = RegInit(0.U.asTypeOf(new MemUnitIn))
  val pWbReg = RegInit(0.U.asTypeOf(new WbUnitIn))


  when(io.enable) {
    pExReg := io.in
    pMemReg := io.memIn
    pWbReg := io.wbIn
    when(io.flush){
      pMemReg.memWrite := false.B
      pWbReg.finishFlag := false.B
      pWbReg.regWriteEnable := false.B
      pExReg.pcOpcode := 0.U
    }
  }

  io.memOut := pMemReg
  io.memOut.address := io.out.res
  io.wbOut := pWbReg
  io.wbOut.regWriteData := io.out.res

  alu.io.inA := pExReg.inA
  alu.io.inB := pExReg.inB
  alu.io.opcode := pExReg.opcode
  io.out.res := alu.io.out

  when(pExReg.pcAdd) {
    io.out.jumpAddress := pExReg.pc + pExReg.pcImm
  }.otherwise{
    io.out.jumpAddress := pExReg.pcImm
  }

  io.out.jump := false.B
  when(pExReg.pcOpcode === 1.U){
    io.out.jump := alu.io.flagZero
  }.elsewhen(pExReg.pcOpcode === 2.U){
    io.out.jump := alu.io.flagCarry
  }.elsewhen(pExReg.pcOpcode === 3.U){
    io.out.jump := alu.io.flagCarry||alu.io.flagZero
  }.elsewhen(pExReg.pcOpcode === 4.U){
    io.out.jump := true.B
  }.elsewhen(pExReg.pcOpcode === 5.U){
    io.out.jump := !alu.io.flagZero
  }.elsewhen(pExReg.pcOpcode === 6.U){
    io.out.jump := (alu.io.flagSign != alu.io.flagOverflow)
  }.elsewhen(pExReg.pcOpcode === 7.U){
    io.out.jump := (alu.io.flagSign != alu.io.flagOverflow)||alu.io.flagZero
  }
  //printf("[EX] FLAGS Carry:%d Sign:%d Zero:%d OverFlow:%d\n", flagCarry, flagSign, flagZero, flagOverflow)

  when(conf.debugEx.B) {
    printf("[EX] opcode:0x%x\n", pExReg.opcode)
    printf("[EX] inA:0x%x\n", pExReg.inA)
    printf("[EX] inB:0x%x\n", pExReg.inB)
    printf("[EX] Res:0x%x\n", io.out.res)
    printf("[EX] PC Address:0x%x\n", pExReg.pc)
    printf("[EX] Jump:%d\n", io.out.jump)
    printf("[EX] JumpAddress:0x%x\n", io.out.jumpAddress)
  }
}
object ALUOpcode {
  def ADD = BitPat("b0000")
  def SUB = BitPat("b0001")
  def AND = BitPat("b0010")
  def XOR = BitPat("b0011")
  def OR  = BitPat("b0100")
  def LSL = BitPat("b0101")
  def LSR = BitPat("b0110")
  def ASR = BitPat("b0111")
  def MOV = BitPat("b1000")
}
