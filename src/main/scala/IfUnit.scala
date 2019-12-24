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

import chisel3.{util, _}
import chisel3.util.Cat

object romCacheStateType {
  val NotLoaded = 0.U(1.W)
  val Loaded = 1.U(1.W)
}

class DependencySolverPort(implicit val conf:CAHPConfig) extends Bundle {
  val instA = Input(UInt(24.W))
  val instB = Input(UInt(24.W))
  val execA = Output(Bool())
  val execB = Output(Bool())
}

class DependencySolver(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new DependencySolverPort())

  val instARegWrite = DecoderUtils.getRegWrite(io.instA)
  val instBRegWrite = DecoderUtils.getRegWrite(io.instB)
  val instARd = DecoderUtils.getRegRd(io.instA)
  val instBRd = DecoderUtils.getRegRd(io.instB)


  io.execA := true.B
  when(io.instA(2,1) === InstructionCategory.InstJ){
    io.execB := false.B
  }.elsewhen(io.instA(2,1) === InstructionCategory.InstM){
    when(io.instB(2,1) === InstructionCategory.InstM){
      io.execB := false.B
    }.otherwise{
      when(instARegWrite && instBRegWrite && (instARd === instBRd)){
        io.execB := false.B
      }.otherwise{
        io.execB := true.B
      }
    }
  }.otherwise{
    when(instARegWrite && instBRegWrite && (instARd === instBRd)){
      io.execB := false.B
    }.otherwise{
      io.execB := true.B
    }
  }
}

class IfUnitIn(implicit val conf: CAHPConfig) extends Bundle {
  val romData = Input(UInt(32.W))
  val jumpAddress = Input(UInt(conf.romAddrWidth.W))
  val jump = Input(Bool())
}

class IfUnitOut(implicit val conf: CAHPConfig) extends Bundle {
  val pcAddress = Output(UInt(conf.romAddrWidth.W))
  val romAddress = Output(UInt((conf.romAddrWidth-2).W))
  val instOut = Output(UInt(24.W))
  val romData = Output(UInt(32.W))
  val stole = Output(Bool())
}

class IfUnitPort(implicit val conf: CAHPConfig) extends Bundle {
  val in = new IfUnitIn
  val out = new IfUnitOut
  val enable = Input(Bool())

  val testRomCacheState = if (conf.test) Output(Bool()) else Output(UInt(0.W))
  val testRomCache = if (conf.test) Output(UInt(32.W)) else Output(UInt(0.W))
}

class IfUnit(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new IfUnitPort)
  val pc = Module(new PC)
  val stole = Wire(Bool())
  val romData = Wire(UInt(32.W))

  // **** Register Declaration ****
  val romCache = Reg(UInt(32.W))
  val romCacheState = RegInit(romCacheStateType.NotLoaded)


  // **** I/O Connection ****
  pc.io.jumpAddress := io.in.jumpAddress
  pc.io.jump := io.in.jump
  pc.io.enable := io.enable
  pc.io.longInst := io.out.instOut(0) === 1.U

  io.out.pcAddress := pc.io.pcOut
  io.out.stole := stole

  // **** Test I/O Connection ****
  io.testRomCacheState := romCacheState
  io.testRomCache := romCache



  // **** Combination Circuit ****
  def getInstOpByte(pc:UInt, block:UInt):UInt = {
    val res = Wire(UInt(8.W))
    when(pc(1,0) === 0.U){
      res := block(7, 0)
    }.elsewhen(pc(1,0) === 1.U){
      res := block(15, 8)
    }.elsewhen(pc(1,0) === 2.U){
      res := block(23, 16)
    }.otherwise {
      res := block(31, 24)
    }
    res
  }

  def getInst(pc:UInt, upperBlock:UInt, lowerBlock:UInt):UInt = {
    val inst = Wire(UInt(24.W))
    when(pc(1,0) === 0.U){
      inst := Cat(lowerBlock(23, 16), lowerBlock(15, 8), lowerBlock(7, 0))
    }.elsewhen(pc(1,0) === 1.U){
      inst := Cat(lowerBlock(31, 24), lowerBlock(23, 16), lowerBlock(15, 8))
    }.elsewhen(pc(1,0) === 2.U){
      inst := Cat(upperBlock(7, 0), lowerBlock(31, 24), lowerBlock(23, 16))
    }.otherwise {
      inst := Cat(upperBlock(15, 8), upperBlock(7, 0), lowerBlock(31, 24))
    }
    inst
  }

  // **** Sequential Circuit ****
  romData := io.in.romData
  io.out.romData := romData
  stole := false.B
  when(io.enable) {
    when(io.in.jump) {
      io.out.romAddress := io.in.jumpAddress(conf.romAddrWidth - 1, 2)
      romCache := romData
      val isLong = getInstOpByte(io.in.jumpAddress, romData)(0) === 1.U
      when(isLong) {
        when(io.in.jumpAddress(1, 0) === 1.U) {
          romCacheState := romCacheStateType.NotLoaded
          stole := false.B
        }.elsewhen(io.in.jumpAddress(1) === 1.U) {
          romCacheState := romCacheStateType.Loaded
          stole := true.B
        }.otherwise {
          romCacheState := romCacheStateType.Loaded
          stole := false.B
        }
      }.otherwise {
        when(io.in.jumpAddress(1, 0) === 2.U) {
          romCacheState := romCacheStateType.NotLoaded
          stole := false.B
        }.elsewhen(io.in.jumpAddress(1, 0) === 3.U) {
          romCacheState := romCacheStateType.Loaded
          stole := true.B
        }.otherwise {
          romCacheState := romCacheStateType.Loaded
          stole := false.B
        }
      }
      when(stole){
        io.out.instOut := 0.U
      }.otherwise {
        io.out.instOut := getInst(io.out.pcAddress, romData, romData)
      }
    }.otherwise {
      when(romCacheState === romCacheStateType.NotLoaded) {
        val isLong = getInstOpByte(io.out.pcAddress, romData)(0) === 1.U
        io.out.romAddress := io.out.pcAddress(conf.romAddrWidth - 1, 2)
        romCache := romData
        when(isLong) {
          when(io.out.pcAddress(1, 0) === 1.U) {
            romCacheState := romCacheStateType.NotLoaded
            stole := false.B
          }.elsewhen(io.out.pcAddress(1) === 1.U) {
            romCacheState := romCacheStateType.Loaded
            stole := true.B
          }.otherwise {
            romCacheState := romCacheStateType.Loaded
            stole := false.B
          }
        }.otherwise {
          when(io.out.pcAddress(1, 0) === 2.U) {
            romCacheState := romCacheStateType.NotLoaded
            stole := false.B
          }.elsewhen(io.out.pcAddress(1, 0) === 3.U) {
            romCacheState := romCacheStateType.Loaded
            stole := true.B
          }.otherwise {
            romCacheState := romCacheStateType.Loaded
            stole := false.B
          }
        }
        when(stole){
          io.out.instOut := 0.U
        }.otherwise{
          io.out.instOut := getInst(io.out.pcAddress, romData, romData)
        }
      }.otherwise {
        io.out.romAddress := io.out.pcAddress(conf.romAddrWidth - 1, 2) + 1.U
        romCacheState := romCacheStateType.Loaded
        val isLong = getInstOpByte(io.out.pcAddress, romCache)(0) === 1.U
        when(isLong) {
          when(io.out.pcAddress(1, 0) === 0.U) {
            romCache := romCache
          }.otherwise {
            romCache := romData
          }
        }.otherwise {
          when(io.out.pcAddress(1) === 0.U) {
            romCache := romCache
          }.otherwise {
            romCache := romData
          }
        }
        io.out.instOut := getInst(io.out.pcAddress, romData, romCache)
      }
    }
  }.otherwise{
    io.out.instOut := DontCare
    io.out.romAddress := DontCare
  }
  when(conf.debugIf.B){
    printf("\n[IF]PC Address:0x%x\n", io.out.pcAddress)
    printf("[IF] Instruction Out:%x\n", io.out.instOut)
    printf("[IF] Stole:%d\n", io.out.stole)
    printf("[IF] RomCacheState:%x\n", romCacheState)
    printf("[IF] jump:%d\n", io.in.jump)
    printf("[IF] JumpAddress:0x%x\n", io.in.jumpAddress)
    printf("[IF] RomAddress:%d\n", io.out.romAddress)
    printf("[IF] RomData:0x%x\n", romData)
    printf("[IF] RomCache:0x%x\n", romCache)
  }
}

class PCPort(implicit val conf: CAHPConfig) extends Bundle {
  val jumpAddress = Input(UInt(conf.romAddrWidth.W))
  val longInst = Input(Bool())
  val jump = Input(Bool())
  val enable = Input(Bool())
  val pcOut = Output(UInt(conf.romAddrWidth.W))
}

class PC(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new PCPort)

  //**** Register Declaration ****
  val regPC = RegInit(0.U(conf.romAddrWidth.W))


  //**** I/O Connection ****
  io.pcOut := regPC


  //**** Sequential Circuit ****
  regPC := regPC
  when(io.enable) {
    when(io.jump === false.B) {
      when(io.longInst === true.B){
        regPC := regPC + 3.U
      }.otherwise{
        regPC := regPC + 2.U
      }
    }.otherwise {
      regPC := io.jumpAddress
    }
  }
}
