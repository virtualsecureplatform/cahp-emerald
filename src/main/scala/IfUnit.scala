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

  val romData = Output(UInt(64.W))
  val stole = Output(Bool())
}

class IfUnitPort(implicit val conf: CAHPConfig) extends Bundle {
  val in = new IfUnitIn
  val out = new IfUnitOut
  val idStole = Input(Bool())
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
  val instBAddr = Wire(UInt(conf.romAddrWidth.W))
  val instBOut = Wire(UInt(24.W))
  instBOut := DontCare
  val isLong = Wire(Bool())
  val isInstBLong = instBOut(0) === 1.U
  isLong := false.B

  // **** Register Declaration ****
  val romCache = Reg(UInt(64.W))
  val romCacheState = RegInit(romCacheStateType.NotLoaded)


  // **** I/O Connection ****
  pc.io.jumpAddress := io.in.jumpAddress
  pc.io.jump := io.in.jump
  pc.io.enable := io.enable&(!stole)

  when(depSolver.io.execB){
    io.out.pcAddress := instBAddr
  }.otherwise{
    io.out.pcAddress := pc.io.pcOut
  }

  io.out.stole := stole

  depSolver.io.instA := io.out.instAOut
  depSolver.io.instB := instBOut

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
  stole := io.idStole

  when(isLong){
    when(depSolver.io.execB) {
      when(isInstBLong){
        pc.io.pcDiff := 6.U
      }.otherwise{
        pc.io.pcDiff := 5.U
      }
    }.otherwise{
      pc.io.pcDiff := 3.U
    }
  }.otherwise{
    when(depSolver.io.execB) {
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
        instBOut := 0.U
      }.otherwise {
        io.out.instOut := getInst(io.in.jumpAddress, romData, romData)
        io.out.instAOut := getInst(io.in.jumpAddress, romData, romData)
        when(instBLoadable){
          instBOut := getInst(instBAddr, romData, romData)
        }.otherwise{
          instBOut := 0.U
        }
      }
    }.otherwise {
      when(romCacheState === romCacheStateType.NotLoaded) {
        isLong := getInstOpByte(pc.io.pcOut, romData)(0) === 1.U
        io.out.romAddress := pc.io.pcOut(conf.romAddrWidth - 2, 3)
        romCache := romData
        when(isLong) {
          when(pc.io.pcOut(2, 0) === 5.U) {
            romCacheState := romCacheStateType.NotLoaded
            stole := false.B
          }.elsewhen(pc.io.pcOut(2, 1) === 3.U) {
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
          when(pc.io.pcOut(2, 0) === 6.U) {
            romCacheState := romCacheStateType.NotLoaded
            stole := false.B
          }.elsewhen(pc.io.pcOut(2, 0) === 7.U) {
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
          instBOut := 0.U
        }.otherwise{
          io.out.instOut := getInst(pc.io.pcOut, romData, romData)
          io.out.instAOut := getInst(io.in.jumpAddress, romData, romData)
          when(instBLoadable){
            instBOut := getInst(instBAddr, romData, romData)
          }.otherwise{
            instBOut := 0.U
          }
        }
      }.otherwise {
        instBLoadable := true.B
        io.out.romAddress := pc.io.pcOut(conf.romAddrWidth - 2, 3) + 1.U
        romCacheState := romCacheStateType.Loaded
        isLong := getInstOpByte(pc.io.pcOut, romCache)(0) === 1.U
        val instBLoadFromCache = Wire(Bool())
        instBLoadFromCache := true.B
        when(isLong) {
          instBAddr := pc.io.pcOut + 3.U
          when(pc.io.pcOut(2, 0) === 5.U || pc.io.pcOut(2, 0) === 6.U || pc.io.pcOut(2, 0) === 7.U) {
            romCache := romData
            instBLoadFromCache := false.B
          }.elsewhen(pc.io.pcOut(2, 0) === 4.U){
            romCache := romData
          }.otherwise {
            romCache := romCache
          }
        }.otherwise {
          instBAddr := pc.io.pcOut + 2.U
          when(pc.io.pcOut(2, 0) === 6.U || pc.io.pcOut(2, 0) === 7.U) {
            romCache := romData
            instBLoadFromCache := false.B
          }.elsewhen(pc.io.pcOut(2, 0) === 5.U){
            romCache := romData
          }.otherwise {
            romCache := romCache
          }
        }
        io.out.instOut := getInst(pc.io.pcOut, romData, romCache)
        io.out.instAOut := getInst(pc.io.pcOut, romData, romCache)
        when(instBLoadFromCache){
          instBOut := getInst(instBAddr, romData, romCache)
        }.otherwise{
          instBOut := getInst(instBAddr, romData, romData)
        }
      }
    }
  }.otherwise{
    io.out.instOut := DontCare
    io.out.instAOut := DontCare
    instBOut := DontCare
    io.out.romAddress := DontCare
  }

  when(depSolver.io.execB){
    io.out.instBOut := instBOut
  }.otherwise{
    io.out.instBOut := 0.U
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
