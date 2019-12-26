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
  val instBLong = io.instB(0) === 1.U

  val instBRd = io.instB(11, 8)
  val instBRs1 = io.instB(15, 12)
  val instBRs2 = io.instB(19, 16)


  io.execA := true.B
  io.execB := true.B
  when(io.instA(2,1) === InstructionCategory.InstJ){
    io.execB := false.B
  }.elsewhen(io.instA(2,1) === InstructionCategory.InstM){
    when(io.instB(2,1) === InstructionCategory.InstM){
      io.execB := false.B
    }.otherwise{
      when(instBLong){
        //LI
        when(io.instB(5,0) === "b110101".U) {
          when(instBRd === instARd){
            io.execB := false.B
          }
        }.elsewhen(io.instB(2,1) === InstructionCategory.InstM){
          when((instBRd === instARd) || (instBRs1 === instARd )){
            io.execB := false.B
          }
        }.elsewhen(io.instB(2,1) === InstructionCategory.InstR){
          when((instBRd === instARd) || (instBRs1 === instARd )|| (instBRs2 === instARd)){
            io.execB := false.B
          }
        }.elsewhen(io.instB(2,1) === InstructionCategory.InstI){
          when((instBRd === instARd) || (instBRs1 === instARd )){
            io.execB := false.B
          }
        }.otherwise{
          when((instBRd === instARd) || (instBRs1 === instARd )){
            io.execB := false.B
          }
        }
      }.otherwise {
        when(io.instB(2, 1) === InstructionCategory.InstM) {
          when((instBRd === instARd)) {
            io.execB := false.B
          }
        }.elsewhen(io.instB(2, 1) === InstructionCategory.InstR) {
          when((instBRd === instARd) || (instBRs1 === instARd)) {
            io.execB := false.B
          }
        }.elsewhen(io.instB(2, 1) === InstructionCategory.InstI) {
          when((instBRd === instARd)) {
            io.execB := false.B
          }
        }.otherwise {
          //JALR, JR
          when((instBRd === instARd) && io.instB(3) === 0.U) {
            io.execB := false.B
          }
        }
      }
    }
  }.otherwise{
    when(instBLong){
      //LI
      when(io.instB(5,0) === "b110101".U) {
        when(instBRd === instARd){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstM){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstR){
        when((instBRd === instARd) || (instBRs1 === instARd )|| (instBRs2 === instARd)){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstI){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }.otherwise{
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }
    }.otherwise{
      when(io.instB(2,1) === InstructionCategory.InstM){
        when((instBRd === instARd)){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstR){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstI){
        when((instBRd === instARd)){
          io.execB := false.B
        }
      }.otherwise{
        //JALR, JR
        when((instBRd === instARd) && io.instB(3) === 0.U){
          io.execB := false.B
        }
      }
    }
  }
}

class IfUnitIn(implicit val conf: CAHPConfig) extends Bundle {
  val romData = Input(UInt(64.W))
  val jumpAddress = Input(UInt(conf.romAddrWidth.W))
  val jump = Input(Bool())
}

class IfUnitOut(implicit val conf: CAHPConfig) extends Bundle {
  val pcAddress = Output(UInt(conf.romAddrWidth.W))
  val romAddress = Output(UInt((conf.romAddrWidth-3).W))
  val instOut = Output(UInt(24.W))

  val instAOut = Output(UInt(24.W))
  val instBOut = Output(UInt(24.W))
  val execB = Output(Bool())

  val romData = Output(UInt(64.W))
  val stole = Output(Bool())
}

class IfUnitPort(implicit val conf: CAHPConfig) extends Bundle {
  val in = new IfUnitIn
  val out = new IfUnitOut
  val enable = Input(Bool())

  val testRomCacheState = if (conf.test) Output(Bool()) else Output(UInt(0.W))
  val testRomCache = if (conf.test) Output(UInt(64.W)) else Output(UInt(0.W))
}

class IfUnit(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new IfUnitPort)
  val pc = Module(new PC)
  val depSolver = Module(new DependencySolver())


  val stole = Wire(Bool())
  val romData = Wire(UInt(64.W))
  val instBLoadable = Wire(Bool())

  // **** Register Declaration ****
  val romCache = Reg(UInt(64.W))
  val romCacheState = RegInit(romCacheStateType.NotLoaded)


  // **** I/O Connection ****
  pc.io.jumpAddress := io.in.jumpAddress
  pc.io.jump := io.in.jump
  pc.io.enable := io.enable

  io.out.pcAddress := pc.io.pcOut
  io.out.stole := stole

  depSolver.io.instA := io.out.instAOut
  depSolver.io.instB := io.out.instBOut
  io.out.execB := depSolver.io.execB

  // **** Test I/O Connection ****
  io.testRomCacheState := romCacheState
  io.testRomCache := romCache



  // **** Combination Circuit ****
  def getInstOpByte(pc:UInt, block:UInt):UInt = {
    val res = Wire(UInt(8.W))
    when(pc(2,0) === 0.U){
      res := block(7, 0)
    }.elsewhen(pc(2,0) === 1.U){
      res := block(15, 8)
    }.elsewhen(pc(2,0) === 2.U){
      res := block(23, 16)
    }.elsewhen(pc(2,0) === 3.U) {
      res := block(31, 24)
    }.elsewhen(pc(2,0) === 4.U) {
      res := block(39, 32)
    }.elsewhen(pc(2,0) === 5.U) {
      res := block(47, 40)
    }.elsewhen(pc(2,0) === 6.U) {
      res := block(55, 48)
    }.otherwise {
      res := block(63, 56)
    }
    res
  }

  def getInst(pc:UInt, upperBlock:UInt, lowerBlock:UInt):UInt = {
    val inst = Wire(UInt(24.W))
    when(pc(2,0) === 0.U){
      inst := Cat(lowerBlock(23, 16), lowerBlock(15, 8), lowerBlock(7, 0))
    }.elsewhen(pc(2,0) === 1.U){
      inst := Cat(lowerBlock(31, 24), lowerBlock(23, 16), lowerBlock(15, 8))
    }.elsewhen(pc(2,0) === 2.U){
      inst := Cat(lowerBlock(39, 32), lowerBlock(31, 24), lowerBlock(23, 16))
    }.elsewhen(pc(2,0) === 3.U){
      inst := Cat(lowerBlock(47, 40), lowerBlock(39, 32), lowerBlock(31, 24))
    }.elsewhen(pc(2,0) === 4.U){
      inst := Cat(lowerBlock(55, 48), lowerBlock(47, 40), lowerBlock(39, 32))
    }.elsewhen(pc(2,0) === 5.U){
      inst := Cat(lowerBlock(63, 56), lowerBlock(55, 48), lowerBlock(47, 40))
    }.elsewhen(pc(2,0) === 6.U){
      inst := Cat(upperBlock(7, 0), lowerBlock(63, 56), lowerBlock(55, 48))
    }.otherwise {
      inst := Cat(upperBlock(15, 8), upperBlock(7, 0), lowerBlock(63, 56))
    }
    inst
  }

  // **** Sequential Circuit ****
  romData := io.in.romData
  io.out.romData := romData

  instBLoadable := false.B
  stole := false.B
  val instBAddr = Wire(UInt(conf.romAddrWidth.W))
  val isLong = Wire(Bool())
  val isInstBLong = io.out.instBOut(0) === 1.U
  isLong := false.B

  when(isLong){
    when(io.out.execB) {
      when(isInstBLong){
        pc.io.pcDiff := 6.U
      }.otherwise{
        pc.io.pcDiff := 5.U
      }
    }.otherwise{
      pc.io.pcDiff := 3.U
    }
  }.otherwise{
    when(io.out.execB) {
      when(isInstBLong){
        pc.io.pcDiff := 5.U
      }.otherwise{
        pc.io.pcDiff := 4.U
      }
    }.otherwise{
      pc.io.pcDiff := 2.U
    }
  }

  instBAddr := DontCare
  when(io.enable) {
    when(io.in.jump) {
      io.out.romAddress := io.in.jumpAddress(conf.romAddrWidth - 2, 3)
      romCache := romData
      isLong := getInstOpByte(io.in.jumpAddress, romData)(0) === 1.U
      when(isLong) {
        when(io.in.jumpAddress(2, 0) === 5.U) {
          romCacheState := romCacheStateType.NotLoaded
          stole := false.B
        }.elsewhen(io.in.jumpAddress(2, 1) === 3.U) {
          romCacheState := romCacheStateType.Loaded
          stole := true.B
        }.otherwise {
          romCacheState := romCacheStateType.Loaded
          stole := false.B
        }
        instBAddr := io.in.jumpAddress + 3.U
        when(instBAddr(2, 0) === 3.U || instBAddr(2, 0) === 4.U || instBAddr(2, 0) === 5.U){
          instBLoadable := true.B
        }.elsewhen(instBAddr(2, 0) === 6.U){
          val isInstBLong = getInstOpByte(instBAddr, romData)(0) === 1.U
          instBLoadable := !isInstBLong
        }.otherwise{
          instBLoadable := false.B
        }
      }.otherwise {
        when(io.in.jumpAddress(2, 0) === 6.U) {
          romCacheState := romCacheStateType.NotLoaded
          stole := false.B
        }.elsewhen(io.in.jumpAddress(2, 0) === 7.U) {
          romCacheState := romCacheStateType.Loaded
          stole := true.B
        }.otherwise {
          romCacheState := romCacheStateType.Loaded
          stole := false.B
        }
        instBAddr := io.in.jumpAddress + 2.U
        when(instBAddr(2, 0) === 2.U || instBAddr(2, 0) === 3.U || instBAddr(2, 0) === 4.U || instBAddr(2, 0) === 5.U){
          instBLoadable := true.B
        }.elsewhen(instBAddr(2, 0) === 6.U){
          val isInstBLong = getInstOpByte(instBAddr, romData)(0) === 1.U
          instBLoadable := !isInstBLong
        }.otherwise{
          instBLoadable := false.B
        }
      }
      when(stole){
        io.out.instOut := 0.U
        io.out.instAOut := 0.U
        io.out.instBOut := 0.U
      }.otherwise {
        io.out.instOut := getInst(io.in.jumpAddress, romData, romData)
        io.out.instAOut := getInst(io.in.jumpAddress, romData, romData)
        when(instBLoadable){
          io.out.instBOut := getInst(instBAddr, romData, romData)
        }.otherwise{
          io.out.instBOut := 0.U
        }
      }
    }.otherwise {
      when(romCacheState === romCacheStateType.NotLoaded) {
        isLong := getInstOpByte(io.out.pcAddress, romData)(0) === 1.U
        io.out.romAddress := io.out.pcAddress(conf.romAddrWidth - 2, 3)
        romCache := romData
        when(isLong) {
          when(io.out.pcAddress(2, 0) === 5.U) {
            romCacheState := romCacheStateType.NotLoaded
            stole := false.B
          }.elsewhen(io.out.pcAddress(2, 1) === 3.U) {
            romCacheState := romCacheStateType.Loaded
            stole := true.B
          }.otherwise {
            romCacheState := romCacheStateType.Loaded
            stole := false.B
          }
          instBAddr := pc.io.pcOut + 3.U
          when(instBAddr(2, 0) === 3.U || instBAddr(2, 0) === 4.U || instBAddr(2, 0) === 5.U){
            instBLoadable := true.B
          }.elsewhen(instBAddr(2, 0) === 6.U){
            val isInstBLong = getInstOpByte(instBAddr, romData)(0) === 1.U
            instBLoadable := !isInstBLong
          }.otherwise{
            instBLoadable := false.B
          }
        }.otherwise {
          when(io.out.pcAddress(2, 0) === 6.U) {
            romCacheState := romCacheStateType.NotLoaded
            stole := false.B
          }.elsewhen(io.out.pcAddress(2, 0) === 7.U) {
            romCacheState := romCacheStateType.Loaded
            stole := true.B
          }.otherwise {
            romCacheState := romCacheStateType.Loaded
            stole := false.B
          }
          instBAddr := pc.io.pcOut + 2.U
          when(instBAddr(2, 0) === 2.U || instBAddr(2, 0) === 3.U || instBAddr(2, 0) === 4.U || instBAddr(2, 0) === 5.U){
            instBLoadable := true.B
          }.elsewhen(instBAddr(2, 0) === 6.U){
            val isInstBLong = getInstOpByte(instBAddr, romData)(0) === 1.U
            instBLoadable := !isInstBLong
          }.otherwise{
            instBLoadable := false.B
          }
        }
        when(stole){
          io.out.instOut := 0.U
          io.out.instAOut := 0.U
          io.out.instBOut := 0.U
        }.otherwise{
          io.out.instOut := getInst(io.out.pcAddress, romData, romData)
          io.out.instAOut := getInst(io.in.jumpAddress, romData, romData)
          when(instBLoadable){
            io.out.instBOut := getInst(instBAddr, romData, romData)
          }.otherwise{
            io.out.instBOut := 0.U
          }
        }
      }.otherwise {
        instBLoadable := true.B
        io.out.romAddress := io.out.pcAddress(conf.romAddrWidth - 2, 3) + 1.U
        romCacheState := romCacheStateType.Loaded
        isLong := getInstOpByte(io.out.pcAddress, romCache)(0) === 1.U
        val instBLoadFromCache = Wire(Bool())
        instBLoadFromCache := true.B
        when(isLong) {
          instBAddr := io.out.pcAddress + 3.U
          when(io.out.pcAddress(2, 0) === 5.U || io.out.pcAddress(2, 0) === 6.U || io.out.pcAddress(2, 0) === 7.U) {
            romCache := romData
            instBLoadFromCache := false.B
          }.otherwise {
            romCache := romCache
          }
        }.otherwise {
          instBAddr := io.out.pcAddress + 2.U
          when(io.out.pcAddress(2, 0) === 6.U || io.out.pcAddress(2, 0) === 7.U) {
            romCache := romData
            instBLoadFromCache := false.B
          }.otherwise {
            romCache := romCache
          }
        }
        io.out.instOut := getInst(io.out.pcAddress, romData, romCache)
        io.out.instAOut := getInst(io.out.pcAddress, romData, romCache)
        when(instBLoadFromCache){
          io.out.instBOut := getInst(instBAddr, romData, romCache)
        }.otherwise{
          io.out.instBOut := getInst(instBAddr, romData, romData)
        }
      }
    }
  }.otherwise{
    io.out.instOut := DontCare
    io.out.instAOut := DontCare
    io.out.instBOut := DontCare
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
  val pcDiff = Input(UInt(3.W))
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
      regPC := regPC + io.pcDiff
    }.otherwise {
      regPC := io.jumpAddress
    }
  }
}
