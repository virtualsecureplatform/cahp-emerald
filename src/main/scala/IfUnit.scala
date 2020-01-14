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

class IfUnitIn(implicit val conf: CAHPConfig) extends Bundle {
  val romData = Input(UInt(64.W))
  val jumpAddress = Input(UInt(conf.romAddrWidth.W))
  val jump = Input(Bool())
}

class IfUnitOut(implicit val conf: CAHPConfig) extends Bundle {
  val pcAddress = Output(UInt(conf.romAddrWidth.W))
  val romAddress = Output(UInt((conf.romAddrWidth-3).W))

  val instAOut = Output(UInt(24.W))
  val instBOut = Output(UInt(24.W))
}

class IfUnitPort(implicit val conf: CAHPConfig) extends Bundle {
  val in = new IfUnitIn
  val out = new IfUnitOut
  val idStole = Input(Bool())
  val enable = Input(Bool())

  //val testRomCacheState = if (conf.test) Output(Bool()) else Output(UInt(0.W))
  //val testRomCache = if (conf.test) Output(UInt(64.W)) else Output(UInt(0.W))
}

class IfUnit(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new IfUnitPort)
  val pc = Module(new PC)
  val instFetcher = Module(new InstructionFetcher)


  // **** I/O Connection ****
  pc.io.jumpAddress := io.in.jumpAddress
  pc.io.jump := io.in.jump
  pc.io.enable := io.enable&(!io.idStole)
  pc.io.stole := false.B
  pc.io.pcDiff := instFetcher.io.out.pcDiff

  instFetcher.io.in.romData := io.in.romData
  instFetcher.io.in.pcAddress := pc.io.pcOut
  instFetcher.io.in.jumpAddress := io.in.jumpAddress
  instFetcher.io.in.jump := io.in.jump
  instFetcher.io.enable := io.enable&(!io.idStole)

  io.out.instAOut := instFetcher.io.out.instA
  io.out.instBOut := instFetcher.io.out.instB
  io.out.romAddress := instFetcher.io.out.romAddr
  io.out.pcAddress := instFetcher.io.out.pcAddr

  when(conf.debugIf.B){
    printf("\n[IF]PC Address:0x%x\n", io.out.pcAddress)
    printf("[IF] jump:%d\n", io.in.jump)
    printf("[IF] JumpAddress:0x%x\n", io.in.jumpAddress)
    printf("[IF] RomAddress:%d\n", io.out.romAddress)
  }
}
